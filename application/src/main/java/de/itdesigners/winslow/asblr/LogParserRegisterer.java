package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.LogEntry;
import de.itdesigners.winslow.config.LogParser;
import de.itdesigners.winslow.pipeline.StageAssignedWorkspace;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.resource.ResourceManager;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class LogParserRegisterer implements AssemblerStep {

    public static final    String          PARSER_TYPE_REGEX_MATCHER_CSV = "regex-matcher/csv";
    public static final    String          LOG_PARSER_OUTPUT_DIRECTORY   = ".log_parser_output";
    private final @Nonnull ResourceManager resourceManager;

    public LogParserRegisterer(@Nonnull ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }


    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        context
                .load(StageAssignedWorkspace.class)
                .map(StageAssignedWorkspace::absolutePath)
                .map(Path::of)
                .flatMap(resourceManager::getWorkspace)
                .ifPresent(workDir -> {
                    var parsers = (context.getStageDefinition() instanceof StageWorkerDefinition w
                                   ? w.logParsers().stream()
                                   : Stream.<LogParser>empty())
                            .flatMap(parser -> instantiateConsumer(
                                    context,
                                    workDir.resolve(LOG_PARSER_OUTPUT_DIRECTORY),
                                    parser
                            ))
                            .toList();

                    parsers.forEach(context::addLogListener);
                    context.store(new LogParsers(parsers));
                });
    }

    @Override
    public void revert(@Nonnull Context context) {
        context.load(LogParsers.class).ifPresent(parsers -> parsers.consumers.forEach(context::removeLogListener));
    }

    @Nonnull
    private Stream<Consumer<LogEntry>> instantiateConsumer(
            @Nonnull Context context,
            @Nonnull Path path,
            @Nonnull LogParser parser) {
        if (!PARSER_TYPE_REGEX_MATCHER_CSV.equals(parser.type()) && !parser.type().startsWith(
                PARSER_TYPE_REGEX_MATCHER_CSV + ":")) {
            context.log(
                    Level.SEVERE,
                    "Ignoring parser with unknown type - only '" + PARSER_TYPE_REGEX_MATCHER_CSV + "' is supported at the moment"
            );
            return Stream.empty();
        }

        final Parameters parameters;

        if (parser.type().startsWith(PARSER_TYPE_REGEX_MATCHER_CSV + ":")) {
            parameters = parseParameters(parser.type().substring(PARSER_TYPE_REGEX_MATCHER_CSV.length() + 1));
        } else {
            parameters = new Parameters();
        }

        var parserDestination = path.resolve(parser.destination());
        if (parserDestination.startsWith(path)) {
            try {
                var pattern = Pattern.compile(parser.matcher());
                var formatter = new Formatter(
                        parser.formatter(),
                        context.getSubmission()::getEnvVariable
                );

                boolean dynamicDestination = parser.destination().contains("$");

                final BiFunction<LogEntry, Matcher, Optional<Path>> destinationResolver;

                if (dynamicDestination) {
                    var destinationFormatter = new Formatter(
                            parser.destination(),
                            context.getSubmission()::getEnvVariable
                    );
                    destinationResolver = (entry, matcher) -> {
                        var destinationName = destinationFormatter.format(entry, matcher);
                        var destinationPath = path.resolve(destinationName);
                        return Optional.of(destinationPath).filter(p -> p.startsWith(path));
                    };
                } else {
                    var destination = Optional.of(parserDestination);
                    destinationResolver = (_e, _m) -> destination;
                }

                var lineAmount = Math.max(1, parameters.lines);
                var lineBuffer = new ArrayDeque<String>(lineAmount);

                // todo
                writeToLogFile(parser.formatter(), parserDestination, context);

                return Stream.of(entry -> {
                    while (lineBuffer.size() >= lineAmount) {
                        lineBuffer.removeFirst();
                    }
                    lineBuffer.add(entry.message());

                    var message = String.join(System.lineSeparator(), lineBuffer);
                    var matcher = pattern.matcher(message);

                    if (matcher.find()) {
                        destinationResolver.apply(entry, matcher).ifPresentOrElse(
                                destination -> {
                                    var text = formatter.format(entry, matcher);
                                    writeToLogFile(text, destination, context);
                                },
                                () -> context.log(
                                        Level.WARNING,
                                        "Invalid destination path, at least one line is ignored"
                                )
                        );
                    }
                });
            } catch (PatternSyntaxException pse) {
                context.log(Level.WARNING, "Invalid pattern syntax", pse);
                return Stream.empty();
            }
        } else {
            context.log(Level.WARNING, "Invalid destination path, at least one parser is ignored");
            return Stream.empty();
        }
    }

    private void writeToLogFile(String text, Path destination, Context context) {
        try {
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            Files.write(
                    destination,
                    List.of(text),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            context.log(Level.SEVERE, "Failed to write entry to destination", e);
        }
    }

    private Parameters parseParameters(String parameters) {
        var keyValues = new HashMap<String, String>();
        var result    = new Parameters();

        for (var param : parameters.split(";")) {
            var params = param.split("=", 2);
            if (params.length == 2) {
                keyValues.put(params[0], params[1]);
            } else {
                keyValues.put(params[0], Boolean.TRUE.toString());
            }
        }

        if (keyValues.containsKey("lines")) {
            try {
                result.lines = Integer.parseInt(keyValues.get("lines"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private static class Parameters {
        public int lines;
    }

    private static class LogParsers {
        private final @Nonnull List<Consumer<LogEntry>> consumers;

        private LogParsers(@Nonnull List<Consumer<LogEntry>> consumers) {
            this.consumers = consumers;
        }
    }


    private static class Formatter {
        public static final          char                                                       MARKER        = '$';
        public static final          int                                                        MARKER_LENGTH = 1;
        public static final @Nonnull Map<String, TriConsumer<StringBuilder, LogEntry, Matcher>> GLOBAL_BLOCKS = Map.of(
                "TIMESTAMP", (builder, e, matcher) -> builder.append(escapeGlobalVariable(String.valueOf(e.time()))),
                "SOURCE", (builder, e, matcher) -> builder.append(escapeGlobalVariable(String.valueOf(e.source()))),
                "ERROR", (builder, e, matcher) -> builder.append(escapeGlobalVariable(e.error() ? "1" : "0"))
        );

        private final @Nonnull List<TriConsumer<StringBuilder, LogEntry, Matcher>> block = new ArrayList<>();
        private final @Nonnull Function<String, Optional<String>>                  environment;

        private Formatter(@Nonnull String format, @Nonnull Function<String, Optional<String>> environment) {
            this.environment = environment;
            int prevIndex = 0;
            int nextIndex;
            while ((nextIndex = format.indexOf(MARKER, prevIndex)) >= 0) {
                if (nextIndex > prevIndex) {
                    var substring = format.substring(prevIndex, nextIndex);
                    this.block.add((builder, e, matcher) -> builder.append(escapeStringLiteral(substring)));
                }

                var length    = 0;
                var allDigits = true;
                for (int i = nextIndex + MARKER_LENGTH; i < format.length(); ++i) {
                    var character = format.charAt(i);
                    var digit     = Character.isDigit(character);
                    if (Character.isAlphabetic(character) || '_' == character || digit) {
                        length += 1;
                        allDigits = allDigits && digit;
                    } else {
                        break;
                    }
                }

                if (length > 0) {
                    var substring = format.substring(nextIndex + MARKER_LENGTH, nextIndex + MARKER_LENGTH + length);
                    if (allDigits) {
                        var index = Integer.parseInt(substring);
                        this.block.add((builder, e, matcher) -> {
                            if (index >= 0 && index <= matcher.groupCount()) {
                                builder.append(escapeMatchedVariable(matcher.group(index)));
                            }
                        });
                    } else {
                        this.block.add(globalEnvironmentBlock(substring).orElse(((builder, e, matcher) -> {
                            try {
                                builder.append(escapeMatchedVariable(matcher.group(substring)));
                            } catch (IllegalArgumentException ignored) {
                                ignored.printStackTrace();
                            }
                        })));
                    }
                }

                prevIndex = nextIndex + MARKER_LENGTH + length;
            }
            if (prevIndex < format.length()) {
                var substring = format.substring(prevIndex);
                this.block.add((builder, e, matcher) -> builder.append(escapeStringLiteral(substring)));
            }
        }

        @Nonnull
        public String format(@Nonnull LogEntry entry, @Nonnull Matcher matcher) {
            var builder = new StringBuilder();
            for (var consumer : block) {
                consumer.accept(builder, entry, matcher);
            }
            return builder.toString();
        }

        @Nonnull
        private Optional<TriConsumer<StringBuilder, LogEntry, Matcher>> globalEnvironmentBlock(@Nonnull String name) {
            var upperCase = name.toUpperCase();
            return Optional
                    .ofNullable(GLOBAL_BLOCKS.get(upperCase))
                    .or(() -> environment
                            .apply(name)
                            .or(() -> environment.apply(upperCase))
                            .map(value -> (builder, e, matcher) -> builder.append(escapeGlobalVariable(value)))
                    );
        }

        @Nonnull
        private static String escapeStringLiteral(@Nonnull String input) {
            return input;
        }

        @Nonnull
        private static String escapeGlobalVariable(@Nonnull String input) {
            return escapeMatchedVariable(input);
        }

        @Nonnull
        private static String escapeMatchedVariable(@Nonnull String input) {
            var escaped = input.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
    }
}
