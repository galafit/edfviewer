package com.biorecorder.basechart.chart;

import com.biorecorder.basechart.data.DataSeries;

/**
 * Created by galafit on 2/11/17.
 */
public class XYViewer {
    DataSeries dataSeries;

    public void setData(DataSeries dataSeries) {
        this.dataSeries = dataSeries;
    }

    public int size() {
        return (int)dataSeries.size();
    }

    public double getX(int index) {
        return dataSeries.getXValue(index);
    }

    public double getY(int index) {
        return dataSeries.getYValue(index, 0);
    }

    public Range getYExtremes() {
        return dataSeries.getYExtremes(0);
    }

    public Range getXExtremes() {
        return dataSeries.getXExtremes();
    }

    public long findNearest(double xValue) {
        return dataSeries.findNearestData(xValue);
    }

}
