package de.itdesigners.winslow.web.api;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class CsvLineAggregator {
    private final @Nonnull String         separator;
    private final @Nonnull Config         config;
    private final @Nonnull List<Operator> operators;

    private final @Nonnull List<Operation> operations = new ArrayList<>();

    public CsvLineAggregator(@Nonnull List<Operator> operators) {
        this(',', operators, new Config());
    }

    public CsvLineAggregator(
            @Nonnull List<Operator> operators,
            @Nonnull Config config) {
        this(',', operators, config);
    }

    public CsvLineAggregator(
            char separator,
            @Nonnull List<Operator> operators,
            @Nonnull Config config) {
        this.separator = String.valueOf(separator);
        this.operators = operators;
        this.config    = config;
    }

    @Nonnull
    public Optional<String> aggregate(@Nonnull String line) {
        var result       = Optional.<String>empty();
        var split        = line.split(this.separator);
        var shouldBeNext = false;


        for (int i = 0; i < this.operators.size() && i < split.length; ++i) {
            if (this.operations.size() > i) {
                shouldBeNext |= this.operations.get(i).shouldBeNext(split[i]);
            }
        }

        if (shouldBeNext) {
            result = this.result();
            this.operations.clear();
        }

        for (int i = 0; i < this.operators.size() && i < split.length; ++i) {
            if (this.operations.size() <= i) {
                this.operations.add(this.operators.get(i).toOperation(split[i], this.config));
            } else {
                this.operations.get(i).push(split[i]);
            }
        }

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
        String decimalFormatter      = "%.2f";
        Locale formatterLocale       = Locale.ENGLISH;

        @Nonnull
        public Config setAggregationSpanMillis(long aggregationSpanMillis) {
            this.aggregationSpanMillis = aggregationSpanMillis;
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
        Avg;

        @Nonnull
        public Operation toOperation(@Nonnull String value, @Nonnull Config config) {
            switch (this) {
                case TimeMs:
                    return new Operation() {
                        final long initial = Long.parseLong(value);

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            var current = Long.parseLong(value);
                            return current >= this.initial + config.aggregationSpanMillis;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.valueOf(this.initial);
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
                        double current = Double.parseDouble(value);

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            var v = Double.parseDouble(value);
                            this.current = Math.min(this.current, v);
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.format(config.formatterLocale, config.decimalFormatter, current);
                        }
                    };
                case Max:
                    return new Operation() {
                        double current = Double.parseDouble(value);

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            var v = Double.parseDouble(value);
                            this.current = Math.max(this.current, v);
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.format(config.formatterLocale, config.decimalFormatter, current);
                        }
                    };
                case Avg:
                    return new Operation() {
                        double current = Double.parseDouble(value);
                        double counter = 1;

                        @Override
                        public boolean shouldBeNext(@Nonnull String value) {
                            return false;
                        }

                        @Override
                        public void push(@Nonnull String value) {
                            this.current += Double.parseDouble(value);
                            this.counter += 1;
                        }

                        @Nonnull
                        @Override
                        public String result() {
                            return String.format(
                                    config.formatterLocale,
                                    config.decimalFormatter,
                                    this.current / this.counter
                            );
                        }
                    };
            }
            throw new RuntimeException("Switch is invalid for " + this);
        }

    }

    private interface Operation {
        boolean shouldBeNext(@Nonnull String value);

        void push(@Nonnull String value);

        @Nonnull
        String result();
    }
}
