package de.itdesigners.winslow.web.api;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class CsvLineAggregator {
    private final @Nonnull String         separator;
    private final @Nonnull Config         config;
    private final @Nonnull List<Operator> operators;

    private final @Nonnull List<Operation> operations = new ArrayList<>();

    private long rowCounter = 0;

    public CsvLineAggregator(@Nonnull List<Operator> operators) {
        this(",", operators, new Config());
    }

    public CsvLineAggregator(
            @Nonnull List<Operator> operators,
            @Nonnull Config config) {
        this(",", operators, config);
    }

    public CsvLineAggregator(
            @Nonnull String separator,
            @Nonnull List<Operator> operators,
            @Nonnull Config config) {
        this.separator = separator;
        this.operators = operators;
        this.config    = config;
    }

    @Nonnull
    public Optional<String> aggregate(@Nonnull String line) {
        var result       = Optional.<String>empty();
        var split        = line.split(this.separator);
        var shouldBeNext = false;


        {
            var splitIndex = 0;
            for (var operation : this.operations) {
                var value = split.length > splitIndex ? split[splitIndex] : "";
                shouldBeNext |= operation.shouldBeNext(value);
                splitIndex += operation.columnWidth();
            }
        }

        if (shouldBeNext || (this.config.aggregationSpanRows != null && rowCounter % this.config.aggregationSpanRows == 0)) {
            result = this.result();
            this.operations.clear();
        }

        {
            var splitIndex = 0;
            for (int i = 0; i < this.operators.size(); ++i) {
                var value = split.length > splitIndex ? split[splitIndex] : "";
                if (this.operations.size() <= i) {
                    this.operations.add(this.operators.get(i).toOperation(value, this.config, this.rowCounter));
                } else {
                    this.operations.get(i).push(value);
                }
                splitIndex += this.operations.get(i).columnWidth();
            }
        }

        this.rowCounter += 1;
        return result;
    }

    @Nonnull
    public Optional<String> result() {
        if (this.operations.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.operations
                                       .stream()
                                       .map(Operation::result)
                                       .collect(Collectors.joining(this.separator)));
        }
    }


    public static class Config {
        Long   aggregationSpanMillis = null;
        Long   aggregationSpanRows   = null;
        String decimalFormatter      = "%.2f";
        Locale formatterLocale       = Locale.ENGLISH;

        @Nonnull
        public Config setAggregationSpanMillis(long aggregationSpanMillis) {
            this.aggregationSpanMillis = aggregationSpanMillis;
            return this;
        }

        @Nonnull
        public Config setAggregationSpanRows(long aggregationSpanRows) {
            this.aggregationSpanRows = aggregationSpanRows;
            return this;
        }

        @Nonnull
        public Config setDecimalFormatter(@Nonnull String decimalFormatter) {
            this.decimalFormatter = decimalFormatter;
            return this;
        }

        @Nonnull
        public Config setFormatterLocale(@Nonnull Locale formatterLocale) {
            this.formatterLocale = formatterLocale;
            return this;
        }
    }

    public enum Operator {
        TimeMs,
        Distinct,
        Last,
        Empty,
        Min,
        Max,
        Avg,
        RowNo0,
        RowNo;

        @Nonnull
        public Operation toOperation(@Nonnull String value, @Nonnull Config config, long row) {
            switch (this) {
                case TimeMs:
                    return new Operation() {
                        final String initial = value;

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            try {
                                var initial = Long.parseLong(this.initial);
                                var current = Long.parseLong(value);
                                return config.aggregationSpanMillis != null && current >= initial + config.aggregationSpanMillis;
                            } catch (NumberFormatException ignored) {
                                return true;
                            }
                        }

                        @Override
                        public void push(@Nonnull String value) {
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return this.initial;
                        }
                    };
                case Distinct:
                    return new Operation() {
                        final String initial = value;

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return !Objects.equals(this.initial, value);
                        }

                        @Override
                        public void push(@Nonnull String value) {
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return this.initial;
                        }
                    };
                case Last:
                    return new Operation() {
                        String last = value;

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            this.last = value;
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return this.last;
                        }
                    };
                case Empty:
                    return new Operation() {
                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return "";
                        }
                    };
                case Min:
                    return new Operation() {
                        double current = 0.0;

                        {
                            try {
                                this.current = Double.parseDouble(value);
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            try {
                                var v = Double.parseDouble(value);
                                this.current = Math.min(this.current, v);
                            } catch (NumberFormatException ignored) {
                                this.current = 0;
                            }
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.format(config.formatterLocale, config.decimalFormatter, current);
                        }
                    };
                case Max:
                    return new Operation() {
                        Double current = null;

                        {
                            try {
                                this.current = Double.parseDouble(value);
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            try {
                                var v = Double.parseDouble(value);
                                this.current = Math.max(this.current, v);
                            } catch (NumberFormatException | NullPointerException ignored) {
                                this.current = null;
                            }
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.format(
                                    config.formatterLocale,
                                    config.decimalFormatter,
                                    current != null ? current : 0
                            );
                        }
                    };
                case Avg:
                    return new Operation() {
                        Double current = null;
                        double counter = 1;

                        {
                            try {
                                this.current = Double.parseDouble(value);
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            try {
                                this.current += Double.parseDouble(value);
                                this.counter += 1;
                            } catch (NumberFormatException | NullPointerException ignored) {
                                this.current = null;
                            }
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.format(
                                    config.formatterLocale,
                                    config.decimalFormatter,
                                    this.current != null
                                    ? this.current / this.counter
                                    : 0
                            );
                        }
                    };
                case RowNo0:
                    return new Operation() {
                        @Override
                        public int columnWidth() {
                            return 0;
                        }

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {

                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.valueOf(row);
                        }
                    };
                case RowNo:
                    return new Operation() {
                        @Override
                        public int columnWidth() {
                            return 0;
                        }

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {

                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.valueOf(row + 1);
                        }
                    };
            }
            throw new RuntimeException("Switch is invalid for " + this);
        }

    }

    private interface Operation {
        default int columnWidth() {
            return 1;
        }

        boolean shouldBeNext(@Nonnull String value);

        void push(@Nonnull String value);

        @Nonnull
        String result();
    }
}
