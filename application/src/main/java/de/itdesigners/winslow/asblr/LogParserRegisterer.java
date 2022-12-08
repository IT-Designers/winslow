package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.config.LogParser;
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
import java.util.stream.Collectors;
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
                .getSubmission()
                .getWorkspaceDirectory()
                .map(Path::of)
                .flatMap(resourceManager::getWorkspace)
                .ifPresent(workDir -> {
                    var parsers = context
                            .getStageDefinition()
                            .getLogParsers()
                            .stream()
                            .flatMap(parser -> instantiateConsumer(
                                    context,
                                    workDir.resolve(LOG_PARSER_OUTPUT_DIRECTORY),
                                    parser
                            ))
                            .collect(Collectors.toList());
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
        if (!PARSER_TYPE_REGEX_MATCHER_CSV.equals(parser.getType()) && !parser.getType().startsWith(
                PARSER_TYPE_REGEX_MATCHER_CSV + ":")) {
            context.log(
                    Level.SEVERE,
                    "Ignoring parser with unknown type - only '" + PARSER_TYPE_REGEX_MATCHER_CSV + "' is supported at the moment"
            );
            return Stream.empty();
        }

        final Parameters parameters;

        if (parser.getType().startsWith(PARSER_TYPE_REGEX_MATCHER_CSV + ":")) {
            parameters = parseParameters(parser.getType().substring(PARSER_TYPE_REGEX_MATCHER_CSV.length() + 1));
        } else {
            parameters = new Parameters();
        }

        var parserDestination = path.resolve(parser.getDestination());
        if (parserDestination.startsWith(path)) {
            try {
                var pattern = Pattern.compile(parser.getMatcher());
                var formatter = new Formatter(
                        parser.getFormatter(),
                        context.getSubmission()::getEnvVariable
                );

                boolean dynamicDestination = parser.getDestination().contains("$");

                final BiFunction<LogEntry, Matcher, Optional<Path>> destinationResolver;

                if (dynamicDestination) {
                    var destinationFormatter = new Formatter(
                            parser.getDestination(),
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

                var lines      = Math.max(1, parameters.lines);
                var lineBuffer = new ArrayDeque<String>(lines);

                return Stream.of(entry -> {
                    while (lineBuffer.size() >= lines) {
                        lineBuffer.removeFirst();
                    }
                    lineBuffer.add(entry.getMessage());

                    var message = String.join(System.lineSeparator(), lineBuffer);
                    var matcher = pattern.matcher(message);

                    if (matcher.find()) {
                        destinationResolver.apply(entry, matcher).ifPresentOrElse(
                                destination -> {
                                    try {
                                        if (destination.getParent() != null) {
                                            Files.createDirectories(destination.getParent());
                                        }
                                        Files.write(
                                                destination,
                                                List.of(formatter.format(entry, matcher)),
                                                StandardCharsets.UTF_8,
                                                StandardOpenOption.APPEND,
                                                StandardOpenOption.CREATE
                                        );
                                    } catch (IOException e) {
                                        context.log(Level.SEVERE, "Failed to write to destination", e);
                                    }
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
                "TIMESTAMP", (builder, e, matcher) -> builder.append(e.getTime()),
                "SOURCE", (builder, e, matcher) -> builder.append(e.getSource()),
                "ERROR", (builder, e, matcher) -> builder.append(e.isError() ? 1 : 0)
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
                    this.block.add((builder, e, matcher) -> builder.append(substring));
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
                                builder.append(matcher.group(index));
                            }
                        });
                    } else {
                        this.block.add(independentBlock(substring).orElse(((builder, e, matcher) -> {
                            try {
                                builder.append(matcher.group(substring));
                            } catch (IllegalArgumentException ignored) {
                            }
                        })));
                    }
                }

                prevIndex = nextIndex + MARKER_LENGTH + length;
            }
            if (prevIndex < format.length()) {
                var substring = format.substring(prevIndex);
                this.block.add((builder, e, matcher) -> builder.append(substring));
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
        private Optional<TriConsumer<StringBuilder, LogEntry, Matcher>> independentBlock(@Nonnull String name) {
            var upperCase = name.toUpperCase();
            return Optional
                    .ofNullable(GLOBAL_BLOCKS.get(upperCase))
                    .or(() -> environment
                            .apply(name)
                            .or(() -> environment.apply(upperCase))
                            .map(value -> (builder, e, matcher) -> builder.append(value))
                    );
        }
    }
}
