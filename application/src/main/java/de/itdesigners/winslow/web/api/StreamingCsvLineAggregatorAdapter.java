package de.itdesigners.winslow.web.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamingCsvLineAggregatorAdapter {

    public static final String PARAM_SEPARATOR               = "separator";
    public static final String PARAM_OPERATORS               = "operators";
    public static final String PARAM_OPERATORS_SPLITTER      = ",";
    public static final String PARAM_AGGREGATION_SPAN_MILLIS = "aggregation-span-millis";
    public static final String PARAM_AGGREGATION_SPAN_ROWS   = "aggregation-span-rows";
    public static final String PARAM_MAX_LINES               = "max-lines";

    private final @Nonnull File file;

    private final @Nonnull  CsvLineAggregator.Config         config;
    private final @Nullable String                           separator;
    private final @Nonnull  List<CsvLineAggregator.Operator> operators;
    private final @Nullable Long                             maxLines;

    public StreamingCsvLineAggregatorAdapter(@Nonnull HttpServletRequest request, @Nonnull File file) {
        this.file = file;

        this.config    = new CsvLineAggregator.Config();
        this.separator = request.getParameter(PARAM_SEPARATOR);
        this.operators = Arrays
                .stream(request.getParameter(PARAM_OPERATORS).split(PARAM_OPERATORS_SPLITTER))
                .map(operator -> {
                    for (CsvLineAggregator.Operator op : CsvLineAggregator.Operator.values()) {
                        if (op.toString().equalsIgnoreCase(operator)) {
                            return op;
                        }
                    }
                    return CsvLineAggregator.Operator.Last;
                })
                .collect(Collectors.toList());


        var aggregationSpanMillisParam = request.getParameter(PARAM_AGGREGATION_SPAN_MILLIS);
        if (aggregationSpanMillisParam != null) {
            this.config.setAggregationSpanMillis(Long.parseLong(aggregationSpanMillisParam));
        }

        var aggregationSpanRows = request.getParameter(PARAM_AGGREGATION_SPAN_ROWS);
        if (aggregationSpanRows != null) {
            this.config.setAggregationSpanRows(Long.parseLong(aggregationSpanRows));
        }

        var maxLinesString = request.getParameter(PARAM_MAX_LINES);
        this.maxLines = maxLinesString == null ? null : Long.parseLong(maxLinesString);

    }

    public ResponseEntity<StreamingResponseBody> buildResponseEntity(@Nonnull ResponseEntity.BodyBuilder builder) {
        return builder
                .contentType(MediaType.TEXT_PLAIN)
                .body(outputStream -> {
                    if (maxLines != null) {
                        try (var lines = Files.lines(file.toPath())) {
                            var lineCount     = lines.count();
                            var aggregateRows = lineCount / maxLines;

                            if (lineCount % maxLines > 0) {
                                aggregateRows += 1;
                            }

                            if (aggregateRows > 0) {
                                config.setAggregationSpanRows(aggregateRows);
                            }
                        }
                    }

                    var aggregator = new CsvLineAggregator(separator != null ? separator : ",", operators, config);
                    try (var lines = Files.lines(file.toPath())) {
                        lines
                                .flatMap(l -> aggregator.aggregate(l).stream())
                                .forEachOrdered(line -> {
                                    var newLine = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
                                    var bytes   = line.getBytes(StandardCharsets.UTF_8);
                                    try {
                                        outputStream.write(bytes);
                                        outputStream.write(newLine);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                        aggregator.result().ifPresent(line -> {
                            var bytes = line.getBytes(StandardCharsets.UTF_8);
                            try {
                                outputStream.write(bytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                });
    }
}
