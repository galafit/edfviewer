package com.biorecorder.basechart;

import com.biorecorder.basechart.axis.XAxisPosition;
import com.biorecorder.basechart.axis.YAxisPosition;
import com.biorecorder.basechart.graphics.BColor;
import com.biorecorder.basechart.graphics.Insets;

/**
 * Created by galafit on 31/8/18.
 */
public class NavigableChartConfig {
    private ChartConfig chartConfig;
    private ChartConfig navigatorConfig;
    private ScrollConfig scrollConfig;

    private BColor backgroundColor = BColor.WHITE_OBSCURE;
    private int gap = 0; // between Chart and Preview px
    private Insets spacing = new Insets(0);
    private int navigatorHeightMin = 16; // px

    public NavigableChartConfig() {
        BColor navigatorBgColor = BColor.WHITE_OBSCURE;
        BColor navigatorMarginColor = navigatorBgColor;

        chartConfig = new ChartConfig();
        navigatorConfig = new ChartConfig();
        scrollConfig = new ScrollConfig();

        chartConfig.getYAxisConfig().setTickLabelOutside(false);
        chartConfig.setPrimaryYPosition(YAxisPosition.RIGHT);
        chartConfig.setPrimaryXPosition(XAxisPosition.TOP);

        navigatorConfig.getYAxisConfig().setTickLabelOutside(false);
        navigatorConfig.setPrimaryYPosition(YAxisPosition.RIGHT);
        navigatorConfig.setBackgroundColor(navigatorBgColor);
        navigatorConfig.setMarginColor(navigatorMarginColor);
        navigatorConfig.setStackGap(0);
        navigatorConfig.setDefaultStackWeight(2);
        navigatorConfig.getLegendConfig().setBackgroundColor(navigatorBgColor);
        navigatorConfig.getYAxisConfig().setRoundingEnabled(true);

        BColor scrollColor = BColor.GRAY_LIGHT;
        scrollConfig.setColor(scrollColor);
    }


    public NavigableChartConfig(ChartConfig chartConfig, ChartConfig navigatorConfig, ScrollConfig scrollConfig) {
        this.chartConfig = chartConfig;
        this.navigatorConfig = navigatorConfig;
        this.scrollConfig = scrollConfig;
    }

    public NavigableChartConfig(NavigableChartConfig config) {
        chartConfig = new ChartConfig(config.chartConfig);
        navigatorConfig = new ChartConfig(config.navigatorConfig);
        scrollConfig = new ScrollConfig(config.scrollConfig);

        backgroundColor = config.backgroundColor;
        gap = config.gap;
        spacing = config.spacing;
        navigatorHeightMin = config.navigatorHeightMin;
    }

    public ChartConfig getChartConfig() {
        return chartConfig;
    }

    public ChartConfig getNavigatorConfig() {
        return navigatorConfig;
    }

    public ScrollConfig getScrollConfig() {
        return scrollConfig;
    }

    public BColor getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(BColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    public Insets getSpacing() {
        return spacing;
    }

    public void setSpacing(Insets spacing) {
        this.spacing = spacing;
    }

    public int getNavigatorHeightMin() {
        return navigatorHeightMin;
    }

    public void setNavigatorHeightMin(int navigatorHeightMin) {
        this.navigatorHeightMin = navigatorHeightMin;
    }
}
