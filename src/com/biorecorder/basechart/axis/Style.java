package com.biorecorder.basechart.axis;

import com.biorecorder.basechart.graphics.BColor;
import com.biorecorder.basechart.graphics.BStroke;

/**
 * This class group properties that usually are changed by css styles
 */
public class Style {
    private BColor lineColor =  BColor.GRAY;
    private BColor tickColor = BColor.GRAY;
    private BColor minorTickColor = BColor.GRAY;
    private BColor gridColor = BColor.LIGHT_GRAY; //BColor(100, 100, 100);
    private BColor minorGridColor = BColor.LIGHT_GRAY; //new BColor(80, 80, 80);

    private BStroke axisLineStroke = new BStroke(1);
    private BStroke gridLineStroke = new BStroke(1);
    private BStroke minorGridLineStroke = new BStroke(1, BStroke.DOT);

    private int tickMarkWidth = 1; // px
    private int minorTickMarkWidth = 0; // px

    public Style() {
    }

    public Style(Style style) {
        lineColor = style.lineColor;
        tickColor = style.tickColor;
        minorTickColor = style.minorTickColor;
        gridColor = style.gridColor;
        minorGridColor = style.minorGridColor;

        axisLineStroke = new BStroke(style.axisLineStroke);
        gridLineStroke = new BStroke(style.gridLineStroke);
        minorGridLineStroke = new BStroke(style.minorGridLineStroke);

        tickMarkWidth = style.tickMarkWidth;
        minorTickMarkWidth = style.minorTickMarkWidth;
    }

    public void setLineColor(BColor lineColor) {
        this.lineColor = lineColor;
    }

    public void setTickColor(BColor tickColor) {
        this.tickColor = tickColor;
    }

    public void setMinorTickColor(BColor minorTickColor) {
        this.minorTickColor = minorTickColor;
    }

    public void setGridColor(BColor gridColor) {
        this.gridColor = gridColor;
    }

    public void setMinorGridColor(BColor minorGridColor) {
        this.minorGridColor = minorGridColor;
    }

    public BColor getTickColor() {
        return tickColor;
    }

    public BColor getMinorTickColor() {
        return minorTickColor;
    }

    public BColor getAxisLineColor() {
        return lineColor;
    }

    public BColor getGridColor() {
        return gridColor;
    }

    public BColor getMinorGridColor() {
        return minorGridColor;
    }

    public BStroke getAxisLineStroke() {
        return axisLineStroke;
    }

    public void setAxisLineStroke(BStroke axisLineStroke) {
        this.axisLineStroke = axisLineStroke;
    }

    public BStroke getGridLineStroke() {
        return gridLineStroke;
    }

    public void setGridLineStroke(BStroke gridLineStroke) {
        this.gridLineStroke = gridLineStroke;
    }

    public BStroke getMinorGridLineStroke() {
        return minorGridLineStroke;
    }

    public void setMinorGridLineStroke(BStroke minorGridLineStroke) {
        this.minorGridLineStroke = minorGridLineStroke;
    }

    public int getTickMarkWidth() {
        return tickMarkWidth;
    }

    public void setTickMarkWidth(int tickMarkWidth) {
        this.tickMarkWidth = tickMarkWidth;
    }

    public int getMinorTickMarkWidth() {
        return minorTickMarkWidth;
    }

    public void setMinorTickMarkWidth(int minorTickMarkWidth) {
        this.minorTickMarkWidth = minorTickMarkWidth;
    }
}
