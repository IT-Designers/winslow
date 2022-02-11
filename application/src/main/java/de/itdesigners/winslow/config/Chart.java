package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;

public class Chart {

    private final @Nonnull String title;
    private final @Nonnull String xAxisGroup;
    private final @Nonnull String xAxisMinValue;
    private final @Nonnull String xAxisMaxValue;
    private final @Nonnull String xAxisLabel;
    private final @Nonnull String yAxisGroup;
    private final @Nonnull String yAxisMinValue;
    private final @Nonnull String yAxisMaxValue;
    private final @Nonnull String yAxisLabel;

    public Chart(
            @Nonnull String title,
            @Nonnull String xAxisGroup,
            @Nonnull String xAxisMinValue,
            @Nonnull String xAxisMaxValue,
            @Nonnull String xAxisLabel,
            @Nonnull String yAxisGroup,
            @Nonnull String yAxisMinValue,
            @Nonnull String yAxisMaxValue,
            @Nonnull String yAxisLabel) {
        this.title         = title;
        this.xAxisGroup    = xAxisGroup;
        this.xAxisMinValue = xAxisMinValue;
        this.xAxisMaxValue = xAxisMaxValue;
        this.xAxisLabel    = xAxisLabel;
        this.yAxisGroup    = yAxisGroup;
        this.yAxisMinValue = yAxisMinValue;
        this.yAxisMaxValue = yAxisMaxValue;
        this.yAxisLabel    = yAxisLabel;
    }





}
