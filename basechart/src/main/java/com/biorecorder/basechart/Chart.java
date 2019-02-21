package com.biorecorder.basechart;

import com.biorecorder.basechart.axis.*;
import com.biorecorder.basechart.button.StateListener;
import com.biorecorder.basechart.graphics.*;
import com.biorecorder.basechart.scales.LinearScale;
import com.biorecorder.basechart.scales.Scale;
import com.biorecorder.basechart.themes.DarkTheme;
import com.biorecorder.basechart.traces.*;
import com.sun.istack.internal.Nullable;

import java.util.*;
import java.util.List;

/**
 * Created by hdablin on 24.03.17.
 */
public class Chart {
    private String title;

    private boolean isLegendVisible = true;
    private boolean isMarginFixed = false;

    private ChartConfig chartConfig = new ChartConfig();

    /*
 * 2 X-axis: 0(even) - BOTTOM and 1(odd) - TOP
 * 2 Y-axis for every section(stack): even - LEFT and odd - RIGHT;
 * All LEFT and RIGHT Y-axis are stacked.
 * If there is no trace associated with some axis... this axis is invisible.
 **/
    private List<AxisWrapper> xAxisList = new ArrayList<>(2);
    private List<AxisWrapper> yAxisList = new ArrayList<>();

    private ArrayList<Integer> stackWeights = new ArrayList<Integer>();
    private List<Trace> traces = new ArrayList<Trace>();
    private Legend legend;
    private Title titleText;
    private BRectangle fullArea;
    private BRectangle graphArea;
    private Insets margin;

    private Crosshair crosshair;
    private Tooltip tooltip;

    private TraceCurve selectedCurve;
    private TraceCurvePoint hoverPoint;

    private DataProcessingConfig dataProcessingConfig;

    public Chart() {
        this((new DarkTheme()).getChartConfig());
    }

    public Chart(ChartConfig chartConfig) {
        this(chartConfig, new DataProcessingConfig());
    }


    public Chart(ChartConfig chartConfig1, DataProcessingConfig dataProcessingConfig) {
        this.chartConfig = new ChartConfig(chartConfig1);
        this.dataProcessingConfig = dataProcessingConfig;
        AxisWrapper bottomAxis = new AxisWrapper(new AxisBottom(new LinearScale(), chartConfig.getBottomAxisConfig()));
        AxisWrapper topAxis = new AxisWrapper(new AxisTop(new LinearScale(), chartConfig.getTopAxisConfig()));
        bottomAxis.setRoundingEnabled(chartConfig.isXAxisRoundingEnabled());
        topAxis.setRoundingEnabled(chartConfig.isXAxisRoundingEnabled());
        if (chartConfig.isXAxisRoundingEnabled()) {
            bottomAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingEnabled());
            topAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingEnabled());
        } else {
            bottomAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingDisabled());
            topAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingDisabled());
        }
        if (chartConfig.isBottomAxisPrimary()) {
            bottomAxis.setGridVisible(true);
        } else {
            topAxis.setGridVisible(true);
        }
        xAxisList.add(bottomAxis);
        xAxisList.add(topAxis);

        //legend
        legend = new Legend(chartConfig.getLegendConfig());
        addStack();
    }

    double getBestExtent(int xIndex) {
        double maxExtent = 0;
        for (Trace trace : traces) {
            if (trace.getXScale() == xAxisList.get(xIndex).getScale()) {
                maxExtent = Math.max(maxExtent, trace.getBestExtent(fullArea.width));
            }
        }
        return maxExtent;
    }


    // for all x axis
    BRange getAllTracesFullMinMax() {
        BRange minMax = null;
        for (int i = 0; i < traces.size(); i++) {
            minMax = BRange.join(minMax, traces.get(i).getFullXMinMax());
        }
        return minMax;
    }


    /**
     * 2 Y-axis for every section(stack): even - LEFT and odd - RIGHT;
     */
    private boolean isYAxisLeft(int axisIndex) {
        if ((axisIndex & 1) == 0) { // even (left)
            return true;
        }
        //odd (right)
        return false;
    }

    /**
     * X-axis: 0(even) - BOTTOM and 1(odd) - TOP
     */
    private boolean isXAxisBottom(int axisIndex) {
        if ((axisIndex & 1) == 0) { // even (bottom)
            return true;
        }
        //odd (top)
        return false;
    }

    private void setAreasDirty() {
        graphArea = null;
        margin = null;
    }

    private boolean isAreasDirty() {
        if (margin == null || graphArea == null) {
            return true;
        }
        return false;
    }

    Insets getMargin(BCanvas canvas) {
        if (margin == null) {
            calculateMarginsAndAreas(canvas);
        }
        return margin;
    }

    BRectangle getGraphArea(BCanvas canvas) {
        if (graphArea == null) {
            calculateMarginsAndAreas(canvas);
        }
        return graphArea;
    }

    void setMargin(Insets margin) {
        this.margin = margin;
        setYStartEnd(fullArea.y + margin.top(), fullArea.height - margin.top() - margin.bottom());
        setXStartEnd(fullArea.x + margin.left(), fullArea.width - margin.left() - margin.right());
        graphArea = new BRectangle(fullArea.x + margin.left(), fullArea.y + margin.top(),
                fullArea.width - margin.left() - margin.right(), fullArea.height - margin.top() - margin.bottom());
        if (legend.isAttachedToStacks()) {
            legend.setArea(graphArea);
        }
        titleText = null;
    }

    Scale getXAxisScale(int xIndex) {
        return xAxisList.get(xIndex).getScale();
    }

    private Insets calculateSpacing() {
        if(chartConfig.getSpacing() != null) {
            return chartConfig.getSpacing();
        }
        int minSpacing = 1;
        int spacingTop = minSpacing;
        int spacingBottom = minSpacing;
        int spacingLeft = minSpacing;
        int spacingRight = minSpacing;
        for (int i = 0; i < yAxisList.size(); i++) {
           AxisWrapper axis = yAxisList.get(i);
           if(i % 2 == 0) { // left
              if(axis.isVisible && axis.isTickLabelOutside()) {
                 spacingLeft = chartConfig.getAutoSpacing();
              }
           } else { // right
               if(axis.isVisible && axis.isTickLabelOutside()) {
                   spacingRight = chartConfig.getAutoSpacing();
               }
           }
        }

        for (int i = 0; i < xAxisList.size(); i++) {
            AxisWrapper axis = xAxisList.get(i);
            if(i % 2 == 0) { // bottom
                if(axis.isVisible && axis.isTickLabelOutside()) {
                    spacingBottom = chartConfig.getAutoSpacing();
                }
            } else { // top
                if(axis.isVisible && axis.isTickLabelOutside()) {
                    spacingTop = chartConfig.getAutoSpacing();
                }
            }
        }

        if(title != null){
            spacingTop = 0;
        }
        if(!legend.isAttachedToStacks()) {
            if(legend.isTop()) {
                spacingTop = 0;
            } else if (legend.isBottom()) {
                spacingBottom = 0;
            }
        }
        return new Insets(spacingTop, spacingRight, spacingBottom, spacingLeft);
    }

    void calculateMarginsAndAreas(BCanvas canvas) {
        if (titleText == null) {
            titleText = new Title(title, chartConfig.getTitleConfig(), fullArea, canvas);
        }

        Insets spacing = calculateSpacing();

        int left = 0;
        int right = 0;

        int titleHeight = titleText.getBounds().height;

        int top = spacing.top() + titleHeight + xAxisList.get(1).getWidth(canvas);
        int bottom = spacing.bottom() + xAxisList.get(0).getWidth(canvas);

        int legendHeight = 0;

        if (!legend.isAttachedToStacks()) {
            BRectangle legendArea = new BRectangle(fullArea.x + spacing.left(), fullArea.y + titleHeight + spacing.top(), fullArea.width - spacing.left() - spacing.right(), fullArea.height - titleHeight - spacing.top() - spacing.bottom());
            legend.setArea(legendArea);
            legendHeight = legend.getHeight(canvas);
            if (legend.isTop()) {
                top += legendHeight;
            }
            if (legend.isBottom()) {
                bottom += legendHeight;
            }
        }

        // if margins and graph area were not calculated before or
        // width of some xAxis was changed (never should happen if label rotation angle is 0)
        if (margin == null || margin.top() != top || margin.bottom() != bottom) {
            setYStartEnd(fullArea.y + top, fullArea.height - top - bottom);
        }

        for (int i = 0; i < yAxisList.size(); i++) {
            if (i % 2 == 0) {
                left = Math.max(left, yAxisList.get(i).getWidth(canvas));
            } else {
                right = Math.max(right, yAxisList.get(i).getWidth(canvas));
            }
        }

        left += spacing.left();
        right += spacing.right();

        // if margins and graph area were not calculated before or
        // width of some yAxis was changed
        if (margin == null || margin.left() != left || margin.right() != right) {
            // adjust XAxis ranges
            setXStartEnd(fullArea.x + left, fullArea.width - left - right);
            int topNew = spacing.top() + titleHeight + xAxisList.get(1).getWidth(canvas);
            int bottomNew = spacing.bottom() + xAxisList.get(0).getWidth(canvas);
            if (!legend.isAttachedToStacks()) {
                if (legend.isTop()) {
                    topNew += legendHeight;
                }
                if (legend.isBottom()) {
                    bottomNew += legendHeight;
                }
            }
            if (topNew != top || bottomNew != bottom) {
                // adjust YAxis ranges
                setYStartEnd(fullArea.y + top, fullArea.height - top - bottom);
                top = topNew;
                bottom = bottomNew;
            }
        }

        Insets resultantMargin = new Insets(top, right, bottom, left);
        if (margin == null || !margin.equals(resultantMargin)) {
            margin = resultantMargin;
            graphArea = new BRectangle(fullArea.x + left, fullArea.y + top,
                    fullArea.width - left - right, fullArea.height - top - bottom);

        }
        if (legend.isAttachedToStacks()) {
            legend.setArea(graphArea);
        }

    }

    public int getStacksSumWeight() {
        int weightSum = 0;
        for (Integer weight : stackWeights) {
            weightSum += weight;
        }
        return weightSum;
    }


    private void setXStartEnd(int areaX, int areaWidth) {
        for (AxisWrapper axis : xAxisList) {
            axis.setStartEnd(areaX, areaX + areaWidth);
        }
    }

    private void setYStartEnd(int areaY, int areaHeight) {
        int weightSum = getStacksSumWeight();

        int weightSumTillYAxis = 0;
        int stackCount = yAxisList.size() / 2;
        for (int stack = 0; stack < stackCount; stack++) {
            int yAxisWeight = stackWeights.get(stack);
            int axisHeight = areaHeight * yAxisWeight / weightSum;
            int end = areaY + areaHeight * weightSumTillYAxis / weightSum;
            int start = end + axisHeight;

            yAxisList.get(stack * 2).setStartEnd(start, end);
            yAxisList.get(stack * 2 + 1).setStartEnd(start, end);

            weightSumTillYAxis += stackWeights.get(stack);
        }
    }

    boolean isXAxisUsed(int xIndex) {
        for (Trace trace : traces) {
            if (trace.getXScale() == xAxisList.get(xIndex).getScale()) {
                return true;
            }
        }
        return false;
    }


    public void draw(BCanvas canvas) {
        if (titleText == null) {
            if (titleText == null) {
                titleText = new Title(title, chartConfig.getTitleConfig(), fullArea, canvas);
            }
        }

        if (isAreasDirty()) {
            calculateMarginsAndAreas(canvas);
        }

        canvas.setColor(chartConfig.getMarginColor());
        canvas.fillRect(fullArea.x, fullArea.y, fullArea.width, fullArea.height);

        canvas.setColor(chartConfig.getBackgroundColor());
        canvas.fillRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        canvas.enableAntiAliasAndHinting();

        int topPosition = graphArea.y;
        int bottomPosition = graphArea.y + graphArea.height;
        int leftPosition = graphArea.x;
        int rightPosition = graphArea.x + graphArea.width;

        /*
         * Attention!!!
         * Drawing  axis and grids should be done before drawing traces
         * because this methods invokes axis rounding
         */
        AxisWrapper bottomAxis = xAxisList.get(0);
        AxisWrapper topAxis = xAxisList.get(1);
        if (bottomAxis.isVisible && bottomAxis.isGridVisible) {
            bottomAxis.drawGrid(canvas, bottomPosition, graphArea.height);
        }
        if (topAxis.isVisible && topAxis.isGridVisible) {
            topAxis.drawGrid(canvas, topPosition, graphArea.height);
        }

        AxisWrapper leftAxis;
        AxisWrapper rightAxis;
        for (int i = 0; i < yAxisList.size() / 2; i++) {
            leftAxis = yAxisList.get(i * 2);
            rightAxis = yAxisList.get(i * 2 + 1);
            if (leftAxis.isVisible() && leftAxis.isGridVisible) {
                leftAxis.drawGrid(canvas, leftPosition, graphArea.width);
            }
            if (rightAxis.isVisible() && rightAxis.isGridVisible) {
                rightAxis.drawGrid(canvas, rightPosition, graphArea.width);
            }
        }

        if (bottomAxis.isVisible) {
            bottomAxis.drawAxis(canvas, bottomPosition);
        }
        if (topAxis.isVisible) {
            topAxis.drawAxis(canvas, topPosition);
        }

        for (int i = 0; i < yAxisList.size() / 2; i++) {
            leftAxis = yAxisList.get(i * 2);
            rightAxis = yAxisList.get(i * 2 + 1);
            if (leftAxis.isVisible()) {
                leftAxis.drawAxis(canvas, leftPosition);
            }
            if (rightAxis.isVisible()) {
                rightAxis.drawAxis(canvas, rightPosition);
            }
        }

        canvas.save();
        canvas.setClip(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        for (Trace trace : traces) {
            trace.draw(canvas);
        }
        canvas.restore();

        titleText.draw(canvas);

        if (isLegendVisible) {
            legend.draw(canvas);
        }

        if (hoverPoint != null) {
            crosshair.draw(canvas, graphArea);
            tooltip.draw(canvas, fullArea);
        }
    }

    private int getCurveXIndex(Trace trace) {
        for (int i = 0; i < xAxisList.size(); i++) {
            if (xAxisList.get(i).getScale() == trace.getXScale()) {
                return i;
            }
        }
        return -1;
    }

    private int getCurveYIndex(Trace trace, int curveNumber) {
        for (int i = 0; i < yAxisList.size(); i++) {
            if (yAxisList.get(i).getScale() == trace.getYScale(curveNumber)) {
                return i;
            }
        }
        return -1;
    }


    /**
     * =======================Base methods to interact==========================
     **/

    public void setFixedMargin(Insets margin) {
        if (margin != null) {
            isMarginFixed = true;
            setMargin(margin);
        } else {
            isMarginFixed = false;
            setAreasDirty();
            titleText = null;
        }
    }


    public void setConfig(ChartConfig chartConfig1) {
        this.chartConfig = new ChartConfig(chartConfig1);
        // axis
        for (int i = 0; i < xAxisList.size(); i++) {
            AxisWrapper axis = xAxisList.get(i);
            if (isXAxisBottom(i)) {
                axis.setConfig(chartConfig.getBottomAxisConfig());
            } else {
                axis.setConfig(chartConfig.getTopAxisConfig());
            }
            axis.setRoundingEnabled(chartConfig.isXAxisRoundingEnabled());
            if (chartConfig.isXAxisRoundingEnabled()) {
                axis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingEnabled());
            } else {
                axis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingDisabled());
            }
        }

        for (int i = 0; i < yAxisList.size(); i++) {
            AxisWrapper axis = yAxisList.get(i);
            if (isYAxisLeft(i)) {
                axis.setConfig(chartConfig.getLeftAxisConfig());
            } else {
                axis.setConfig(chartConfig.getRightAxisConfig());
            }
            axis.setRoundingEnabled(chartConfig.isYAxisRoundingEnabled());
            if (chartConfig.isYAxisRoundingEnabled()) {
                axis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingEnabled());
            } else {
                axis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingDisabled());
            }
        }

        //legend
        legend.setConfig(chartConfig.getLegendConfig());

        // title
        titleText = null;
        setAreasDirty();
    }

    public void addStack() {
        addStack(chartConfig.getDefaultStackWeight());
    }

    public void addStack(int weight) {
        AxisWrapper leftAxis = new AxisWrapper(new AxisLeft(new LinearScale(), chartConfig.getLeftAxisConfig()));
        AxisWrapper rightAxis = new AxisWrapper(new AxisRight(new LinearScale(), chartConfig.getRightAxisConfig()));
        leftAxis.setRoundingEnabled(chartConfig.isYAxisRoundingEnabled());
        rightAxis.setRoundingEnabled(chartConfig.isYAxisRoundingEnabled());
        if (chartConfig.isYAxisRoundingEnabled()) {
            leftAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingEnabled());
            rightAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingEnabled());
        } else {
            leftAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingDisabled());
            rightAxis.setRoundingAccuracyPct(chartConfig.getAxisRoundingAccuracyPctIfRoundingDisabled());
        }
        if (chartConfig.isLeftAxisPrimary()) {
            leftAxis.setGridVisible(true);
        } else {
            rightAxis.setGridVisible(true);
        }
        yAxisList.add(leftAxis);
        yAxisList.add(rightAxis);
        stackWeights.add(weight);
        setAreasDirty();
    }

    /**
     * add trace to the last stack
     */
    public void addTrace(Trace trace, boolean isSplit, boolean isXAxisOpposite, boolean isYAxisOpposite) {
        int stackCount = yAxisList.size() / 2;
        addTrace(stackCount - 1, trace, isSplit, isXAxisOpposite, isYAxisOpposite);
    }

    /**
     * add trace to the last stack
     */
    public void addTrace(Trace trace, boolean isSplit) {
        addTrace(trace, isSplit, false, false);
    }

    public void addTrace(int stackNumber, Trace trace, boolean isSplit) {
        addTrace(stackNumber, trace, isSplit, false, false);
    }


    /**
     * add trace to the stack with the given number
     */
    public void addTrace(int stackNumber, Trace trace, boolean isSplit, boolean isXAxisOpposite, boolean isYAxisOpposite) {
        boolean isBottomXAxis = true;
        boolean isLeftYAxis = true;
        if (isXAxisOpposite && chartConfig.isBottomAxisPrimary()) {
            isBottomXAxis = false;
        }
        if (!isXAxisOpposite && !chartConfig.isBottomAxisPrimary()) {
            isBottomXAxis = false;
        }
        if (isYAxisOpposite && chartConfig.isLeftAxisPrimary()) {
            isLeftYAxis = false;
        }
        if (!isYAxisOpposite && !chartConfig.isLeftAxisPrimary()) {
            isLeftYAxis = false;
        }
        int xIndex = isBottomXAxis ? 0 : 1;
        int yIndex = isLeftYAxis ? stackNumber * 2 : stackNumber * 2 + 1;

        trace.setXScale(xAxisList.get(xIndex).getScale());
        xAxisList.get(xIndex).setVisible(true);
        if (!isSplit) {
            AxisWrapper yAxis = yAxisList.get(yIndex);
            yAxis.setVisible(true);
            trace.setYScales(yAxis.getScale());
        } else {
            int stackCount = yAxisList.size() / 2;
            int availableStacks = stackCount - stackNumber;
            if (trace.curveCount() > availableStacks) {
                for (int i = 0; i < trace.curveCount() - availableStacks; i++) {
                    addStack();
                }
            }
            Scale[] yScales = new Scale[trace.curveCount()];
            for (int i = 0; i < trace.curveCount(); i++) {
                AxisWrapper yAxis = yAxisList.get(yIndex + i * 2);
                yScales[i] = yAxis.getScale();
                yAxis.setVisible(true);
            }
            trace.setYScales(yScales);
        }

        BColor[] colors = chartConfig.getTraceColors();
        int totalCurves = 0;
        for (Trace trace1 : traces) {
            totalCurves += trace1.curveCount();
        }
        for (int i = 0; i < trace.curveCount(); i++) {
            if (trace.getCurveColor(i) == null) {
                trace.setCurveColor(i, colors[(totalCurves + i) % colors.length]);
            }
            if (trace.getCurveName(i) == null || trace.getCurveName(i).isEmpty()) {
                trace.setCurveName(i, "Trace" + traces.size() + "_curve" + i);
            }

        }

        traces.add(trace);

        for (int i = 0; i < trace.curveCount(); i++) {
            final int curveNumber = i;
            StateListener traceSelectionListener = new StateListener() {
                @Override
                public void stateChanged(boolean isSelected) {
                    if (isSelected) {
                        selectedCurve = new TraceCurve(trace, curveNumber);
                    }
                    if (!isSelected && selectedCurve.getTrace() == trace && selectedCurve.getCurveNumber() == curveNumber) {
                        selectedCurve = null;
                    }
                }
            };
            legend.add(trace, i, traceSelectionListener);
        }
    }

    public void removeTrace(int traceNumber) {
        legend.remove(traces.get(traceNumber));
        traces.remove(traceNumber);
    }

    public void setArea(BRectangle area) {
        fullArea = area;
        setXStartEnd(area.x, area.width);
        setYStartEnd(area.y, area.height);
        setAreasDirty();
        titleText = null;
    }

    public int traceCount() {
        return traces.size();
    }

    public int xAxisCount() {
        return xAxisList.size();
    }

    public int yAxisCount() {
        return yAxisList.size();
    }

    public void setXMinMax(int xIndex, double min, double max) {
        if (xAxisList.get(xIndex).setMinMax(min, max)) {
            if (xAxisList.get(xIndex).isTickLabelOutside() || !isMarginFixed) {
                setAreasDirty();
            }
        }
    }

    public BRange getXMinMax(int xIndex) {
        return new BRange(xAxisList.get(xIndex).getMin(), xAxisList.get(xIndex).getMax());
    }

    public BRange getYMinMax(int yAxisIndex) {
        return new BRange(yAxisList.get(yAxisIndex).getMin(), yAxisList.get(yAxisIndex).getMax());
    }

    public void setYMinMax(int yAxisIndex, double min, double max) {
        yAxisList.get(yAxisIndex).setMinMax(min, max);
        if (yAxisList.get(yAxisIndex).isTickLabelOutside() || !isMarginFixed) {
            setAreasDirty();
        }
    }

    public void zoomY(int yAxisIndex, double zoomFactor) {
        Scale zoomedScale = yAxisList.get(yAxisIndex).zoom(zoomFactor);
        double zoomedMin = zoomedScale.getDomain()[0];
        double zoomedMax = zoomedScale.getDomain()[zoomedScale.getDomain().length - 1];
        setYMinMax(yAxisIndex, zoomedMin, zoomedMax);
    }

    public void zoomX(int xIndex, double zoomFactor) {
        Scale zoomedScale = xAxisList.get(xIndex).zoom(zoomFactor);
        double zoomedMin = zoomedScale.getDomain()[0];
        double zoomedMax = zoomedScale.getDomain()[zoomedScale.getDomain().length - 1];
        setXMinMax(xIndex, zoomedMin, zoomedMax);
    }

    public void translateY(int yAxisIndex, int translation) {
        Scale translatedScale = yAxisList.get(yAxisIndex).translate(translation);
        double translatedMin = translatedScale.getDomain()[0];
        double translatedMax = translatedScale.getDomain()[translatedScale.getDomain().length - 1];
        setYMinMax(yAxisIndex, translatedMin, translatedMax);
    }

    public void translateX(int xIndex, int translation) {
        Scale translatedScale = xAxisList.get(xIndex).translate(translation);
        double translatedMin = translatedScale.getDomain()[0];
        double translatedMax = translatedScale.getDomain()[translatedScale.getDomain().length - 1];
        setXMinMax(xIndex, translatedMin, translatedMax);
    }

    public void autoScaleX(int xIndex) {
        BRange tracesXMinMax = null;
        for (Trace trace : traces) {
            if (trace.getXScale() == xAxisList.get(xIndex).getScale()) {
                tracesXMinMax = BRange.join(tracesXMinMax, trace.getFullXMinMax());
            }
        }

        if (tracesXMinMax != null) {
            setXMinMax(xIndex, tracesXMinMax.getMin(), tracesXMinMax.getMax());
        }
    }

    public void autoScaleY(int yIndex) {
        BRange tracesYMinMax = null;
        Scale yScale = yAxisList.get(yIndex).getScale();
        for (Trace trace : traces) {
            for (int i = 0; i < trace.curveCount(); i++) {
                if (trace.getYScale(i) == yScale) {
                    tracesYMinMax = BRange.join(tracesYMinMax, trace.curveYMinMax(i));
                }
            }
        }

        if (tracesYMinMax != null) {
            setYMinMax(yIndex, tracesYMinMax.getMin(), tracesYMinMax.getMax());
        }
    }


    public boolean selectCurve(int x, int y) {
        if (legend.selectItem(x, y)) {
            return true;
        }
        return false;
    }

    /**
     * If chart contains the point or point == null then
     * <ul>
     * <li>if selectedCurve != null then return index of selectedCurve X axis</li>
     * <li>if selectedCurve == null then return index of first visible X axis</li>
     * </ul>
     * <p>
     * If point != null but chart does not contain the point then return -1
     */
    public int getXIndex(@Nullable BPoint point) {
        if (selectedCurve != null) {
            return getCurveXIndex(selectedCurve.getTrace());
        }
        if (point != null && graphArea.contains(point.getX(), point.getY())) {
            for (int i = 0; i < xAxisList.size(); i++) {
                if (xAxisList.get(i).isVisible()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * If chart contains the point or point == null then
     * <ul>
     * <li>if selectedCurve != null then return index of selectedCurve  Y axis</li>
     * <li>if selectedCurve == null then return index of visible Y axis belonging to the stack containing the point
     * or just index of first visible Y axis (if point == null)</li>
     * </ul>
     * <p>
     * If point != null but chart does not contain the point then return -1
     */
    public int getYIndex(@Nullable BPoint point) {
        if (selectedCurve != null) {
            return getCurveYIndex(selectedCurve.getTrace(), selectedCurve.getCurveNumber());
        }
        if (point != null && graphArea.contains(point.getX(), point.getY())) {
            for (int stackIndex = 0; stackIndex < yAxisList.size() / 2; stackIndex++) {
                int leftYIndex = 2 * stackIndex;
                int rightYIndex = 2 * stackIndex + 1;
                AxisWrapper axisLeft = yAxisList.get(leftYIndex);
                AxisWrapper axisRight = yAxisList.get(rightYIndex);
                if (axisLeft.getEnd() <= point.getY() && axisLeft.getStart() >= point.getY()) {
                    if (fullArea.x <= point.getX() && point.getX() <= fullArea.x + fullArea.width / 2 && axisLeft.isVisible) { // left half
                        return leftYIndex;
                    }
                    if (fullArea.x + fullArea.width / 2 <= point.getX() && point.getX() <= fullArea.x + fullArea.width) { // right half
                        if (axisRight.isVisible) {
                            return rightYIndex;
                        }
                        return leftYIndex;
                    }
                }
            }
        }

        if (point == null) {
            if (selectedCurve != null) {
                return getCurveYIndex(selectedCurve.getTrace(), selectedCurve.getCurveNumber());
            }

            for (int i = 0; i < yAxisList.size(); i++) {
                if (yAxisList.get(i).isVisible()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean hoverOff() {
        if (hoverPoint != null) {
            hoverPoint = null;
            return true;
        }
        return false;
    }

    public boolean hoverOn(int x, int y) {
        if (!graphArea.contains(x, y)) {
            return hoverOff();
        }
        if (hoverPoint == null && selectedCurve != null) {
            hoverPoint = new TraceCurvePoint(selectedCurve.getTrace(), selectedCurve.getCurveNumber(), -1);
        }

        if (hoverPoint != null) {
            NearestPoint nearestPoint = hoverPoint.getTrace().nearest(x, y, hoverPoint.getCurveNumber());
            if (hoverPoint.getPointIndex() == nearestPoint.getPointIndex()) {
                return false;
            } else {
                hoverPoint = nearestPoint.getCurvePoint();
            }
        } else {
            // find nearest trace curve
            NearestPoint nearestPoint = null;
            for (Trace trace : traces) {
                NearestPoint np = trace.nearest(x, y, -1);
                if (nearestPoint == null || nearestPoint.getDistanceSq() > np.getDistanceSq()) {
                    nearestPoint = np;
                }
            }

            if (nearestPoint != null) {
                hoverPoint = nearestPoint.getCurvePoint();
            }
        }

        if (hoverPoint != null) {
            if (hoverPoint.getPointIndex() >= 0) {
                Trace hoverTrace = hoverPoint.getTrace();
                int hoverCurveNumber = hoverPoint.getCurveNumber();
                int hoverPointIndex = hoverPoint.getPointIndex();
                int xPosition = hoverTrace.xPosition(hoverPointIndex);
                int tooltipYPosition = 0;
                NamedValue xValue = hoverTrace.xValue(hoverPointIndex);

                crosshair = new Crosshair(chartConfig.getCrossHairConfig(), xPosition);
                tooltip = new Tooltip(chartConfig.getTooltipConfig(), xPosition, tooltipYPosition);
                tooltip.setHeader(null, null, xValue.getValueLabel());
                if (chartConfig.isMultiCurveTooltip()) { // all trace curves
                    for (int i = 0; i < hoverTrace.curveCount(); i++) {
                        addCurvePointToTooltip(tooltip, hoverTrace, i, hoverPointIndex);
                        crosshair.addY(hoverTrace.curveYPosition(i, hoverPointIndex));

                    }
                } else { // only hover curve
                    addCurvePointToTooltip(tooltip, hoverTrace, hoverCurveNumber, hoverPointIndex);
                    crosshair.addY(hoverTrace.curveYPosition(hoverCurveNumber, hoverPointIndex));
                }
            }
            return true;
        }
        return false;
    }

    private void addCurvePointToTooltip(Tooltip tooltip, Trace trace, int curveNumber, int pointIndex) {
        NamedValue[] curveValues = trace.curveValues(curveNumber, pointIndex);
        if (curveValues.length == 1) {
            tooltip.addLine(trace.getCurveColor(curveNumber), trace.getCurveName(curveNumber), curveValues[0].getValueLabel());
        } else {
            tooltip.addLine(trace.getCurveColor(curveNumber), trace.getCurveName(curveNumber), "");
            for (NamedValue curveValue : curveValues) {
                tooltip.addLine(null, curveValue.getValueName(), curveValue.getValueLabel());
            }
        }
    }


    /**
     * Implement axis rounding when method:
     * drawAxis or drawGrid or getWidth is invoked !!!
     */
    class AxisWrapper {
        private Axis axis;
        private boolean isVisible = false;
        private boolean isGridVisible = false;
        private boolean isRoundingEnabled = false;
        // need this field to implement smooth zooming and translate when minMaxRounding enabled
        private BRange rowMinMax; // without rounding
        private boolean roundingDirty = true;


        public AxisWrapper(Axis axis) {
            this.axis = axis;
            rowMinMax = new BRange(axis.getMin(), axis.getMax());
        }

        private void setRoundingDirty() {
            roundingDirty = true;
            axis.setMinMax(rowMinMax);
        }

        public void setRoundingAccuracyPct(int roundingAccuracyPct) {
            axis.setRoundingAccuracyPct(roundingAccuracyPct);
            setRoundingDirty();
        }

        private boolean isDirty() {
            if (isRoundingEnabled && roundingDirty) {
                return true;
            }
            return false;
        }

        public void setRoundingEnabled(boolean roundingEnabled) {
            isRoundingEnabled = roundingEnabled;
            setRoundingDirty();
        }

        public boolean isTickLabelOutside() {
            return axis.isTickLabelOutside();
        }

        public void setTitle(String title) {
            axis.setTitle(title);
        }

        public void setTickInterval(double tickInterval) {
            axis.setTickInterval(tickInterval);
            setRoundingDirty();
        }

        public void setMinorTickIntervalCount(int minorTickIntervalCount) {
            axis.setMinorTickIntervalCount(minorTickIntervalCount);
        }

        public void setTickFormatInfo(TickFormatInfo tickFormatInfo) {
            axis.setTickFormatInfo(tickFormatInfo);
            setRoundingDirty();
        }

        public Scale getScale() {
            return axis.getScale();
        }


        public void setConfig(AxisConfig config) {
            axis.setConfig(config);
            setRoundingDirty();
        }


        public Scale zoom(double zoomFactor) {
            // to have smooth zooming we do it on row domain values instead of rounded ones !!!
            axis.setMinMax(rowMinMax);
            return axis.zoom(zoomFactor);
        }


        public Scale translate(int translation) {
            // to have smooth translating we do it on row domain values instead of rounded ones !!!
            axis.setMinMax(rowMinMax);
            Scale scale = axis.translate(translation);
            return scale;
        }

        /**
         * return true if axis min or max actually will be changed
         */
        public boolean setMinMax(double min, double max) {
            if (rowMinMax.getMin() != min || rowMinMax.getMax() != max) {
                rowMinMax = new BRange(min, max);
                setRoundingDirty();
                return true;
            }
            return false;
        }

        /**
         * return true if axis start or end actually changed
         */
        public boolean setStartEnd(int start, int end) {
            if ((int) axis.getStart() != start || (int) axis.getEnd() != end) {
                setRoundingDirty();
                axis.setStartEnd(start, end);
                return true;
            }
            return false;
        }

        public double getMin() {
            return axis.getMin();
        }

        public double getMax() {
            return axis.getMax();
        }

        public int getStart() {
            return (int) axis.getStart();
        }

        public int getEnd() {
            return (int) axis.getEnd();
        }

        public double scale(double value) {
            return axis.scale(value);
        }

        public double invert(float value) {
            return axis.invert(value);
        }


        public boolean isVisible() {
            return isVisible;
        }

        public boolean isGridVisible() {
            return isGridVisible;
        }

        /**
         * this method DO AXIS ROUNDING
         */
        public int getWidth(BCanvas canvas) {
            if (isVisible) {
                if (isDirty()) {
                    axis.roundMinMax(canvas);
                    roundingDirty = false;
                }
                return axis.getWidth(canvas);
            }
            return 0;
        }

        /**
         * this method DO AXIS ROUNDING
         */
        public void drawGrid(BCanvas canvas, int axisOriginPoint, int length) {
            if (isDirty()) {
                axis.roundMinMax(canvas);
                roundingDirty = false;
            }
            axis.drawGrid(canvas, axisOriginPoint, length);
        }

        /**
         * this method DO AXIS ROUNDING
         */
        public void drawAxis(BCanvas canvas, int axisOriginPoint) {
            if (isDirty()) {
                axis.roundMinMax(canvas);
                roundingDirty = false;
            }
            axis.drawAxis(canvas, axisOriginPoint);
        }

        public void setVisible(boolean visible) {
            isVisible = visible;
        }

        public void setGridVisible(boolean gridVisible) {
            isGridVisible = gridVisible;
        }
    }
}
