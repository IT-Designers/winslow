package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record ChartDefinition(
        @Nonnull String name,
        @Nonnull String file,
        @Nonnull Boolean formatterFromHeaderRow,
        @Nonnull String customFormatter,
        @Nonnull String xVariable,
        @Nonnull String xAxisName,
        @Nonnull ChartAxisType xAxisType,
        @Nullable Number xAxisMinValue,
        @Nullable Number xAxisMaxValue,
        @Nonnull String yVariable,
        @Nonnull String yAxisName,
        @Nonnull ChartAxisType yAxisType,
        @Nullable Number yAxisMinValue,
        @Nullable Number yAxisMaxValue,
        @Nullable Integer entryLimit
) {
}
