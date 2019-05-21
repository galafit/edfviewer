package com.biorecorder.basechart;

import com.biorecorder.basechart.graphics.BCanvas;
import com.biorecorder.basechart.graphics.BColor;
import com.biorecorder.basechart.scales.Scale;
import com.biorecorder.data.sequence.StringSequence;


/**
 * Created by galafit on 16/9/17.
 */
public abstract class Trace {
    protected Scale xScale;
    protected Scale[] yScales;
    protected TraceDataManager dataManager;
    protected final int curveCount;
    protected final String[] curveNames;
    protected final BColor[] curveColors;

    public Trace(ChartData data) {
        dataManager = new TraceDataManager(data);
        curveCount = curveCount(data);
        curveNames =curveNames(data);
        curveColors = new BColor[curveCount];
    }

    private ChartData getData() {
        return dataManager.getData(xScale, getMarkSize());
    }

    private final void checkCurveNumber(int curveNumber) {
        if (curveNumber >= curveCount) {
            String errMsg = "Curve = " + curveNumber + " Number of curves: " + curveCount;
            throw new IllegalArgumentException(errMsg);
        }
    }

    public final void appendData() {
        dataManager.appendData();
    }

    public final String getCurveName(int curve) {
        checkCurveNumber(curve);
        return curveNames[curve];
    }

    public final void setCurveName(int curve, String name) {
        checkCurveNumber(curve);
        curveNames[curve] = name;
    }

    public final void setCurveColor(int curve, BColor color) {
        checkCurveNumber(curve);
        curveColors[curve] = color;
    }

    public final BColor getCurveColor(int curve) {
        checkCurveNumber(curve);
        return curveColors[curve];
    }

    public final StringSequence getLabelsIfXColumnIsString() {
        return dataManager.getLabelsIfXColumnIsString();
    }

    public final Range getFullXMinMax() {
        return dataManager.getFullXMinMax(xScale);
    }

    public final double getBestExtent(int drawingAreaWidth) {
        return dataManager.getBestExtent(drawingAreaWidth, getMarkSize());
    }

    public final int curveCount() {
        return curveCount;
    }

    public NamedValue xValue(int dataIndex) {
        double xValue = getData().value(dataIndex, 0);
        return new NamedValue("x: ", xValue, xScale.formatDomainValue(xValue));
    }

    public final NamedValue[] curveValues(int dataIndex, int curve) {
        checkCurveNumber(curve);
        return curveValues(dataIndex, curve, getData());
    }

    public final int curveYPosition(int dataIndex, int curve) {
        checkCurveNumber(curve);
        return curveYPosition(dataIndex, curve, getData());
    }

    public final int curveXPosition(int dataIndex, int curve) {
        checkCurveNumber(curve);
        return curveXPosition(dataIndex, curve, getData());
    }

    public final Range curveYMinMax(int curve) {
        checkCurveNumber(curve);
        return curveYMinMax(curve, getData());
    }

    public final void setDataProcessingConfig(DataProcessingConfig dataProcessingConfig) {
        dataManager.setConfig(dataProcessingConfig);
    }

    public void setXScale(Scale xScale) {
        this.xScale = xScale;
    }

    public void setYScales(Scale... yScales) {
        this.yScales = yScales;
    }

    public Scale getXScale() {
        return xScale;
    }

    public Scale[] getYScales() {
        return yScales;
    }

    public final Scale getYScale(int curve) {
        checkCurveNumber(curve);
        if (curve < yScales.length - 1) {
            return yScales[curve];
        }
        return yScales[yScales.length - 1];
    }

    public int distanceSqw(int pointIndex, int curve, int x, int y) {
        checkCurveNumber(curve);
        int dy = curveYPosition(pointIndex, curve) - y;
        int dx = curveXPosition(pointIndex, curve) - x;
        return dy * dy + dx * dx;
    }

    public TraceCurvePoint nearest(int x, int y, int curve) {
        checkCurveNumber(curve);
        double xValue = xScale.invert(x);
        int pointIndex = dataManager.nearest(xValue);
        if (pointIndex < 0) {
            return null;
        }
        return new TraceCurvePoint(this, curve, pointIndex);
    }

    public TraceCurvePoint nearest(int x, int y) {
        double xValue = xScale.invert(x);
        int pointIndex = dataManager.nearest(xValue);
        if (pointIndex < 0) {
            return null;
        }
        if (curveCount == 1) {
            return new TraceCurvePoint(this, 0, pointIndex);
        }

        int dy_min = 0;
        int closestCurve = 0;
        for (int i = 0; i < curveCount; i++) {
            int dy = Math.abs(curveYPosition(pointIndex, i) - y);
            if (i == 0 || dy_min > dy) {
                dy_min = dy;
                closestCurve = i;
            }
            if (dy_min == 0) {
                break;
            }
        }
        return new TraceCurvePoint(this, closestCurve, pointIndex);
    }

    public final void draw(BCanvas canvas) {
        draw(canvas, getData());
    }

    protected abstract int curveXPosition(int dataIndex, int curve, ChartData data);

    protected abstract int getMarkSize();

    protected abstract int curveYPosition(int dataIndex, int curve, ChartData data);

    protected abstract NamedValue[] curveValues(int dataIndex, int curve, ChartData data);

    protected abstract int curveCount(ChartData data);

    protected abstract String[] curveNames(ChartData data);

    protected abstract Range curveYMinMax(int curveNumber, ChartData data);

    protected abstract void draw(BCanvas canvas, ChartData data);

}
