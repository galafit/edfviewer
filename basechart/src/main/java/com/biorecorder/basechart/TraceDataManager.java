package com.biorecorder.basechart;


import com.biorecorder.basechart.scales.Scale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by galafit on 9/7/18.
 */
public class TraceDataManager {
    private final ChartData traceData;
    private DataProcessingConfig processingConfig;
    private boolean isEqualFrequencyGrouping; // group by equal points number or equal "height"

    private ChartData processedData;
    private List<ChartData> groupedDataList = new ArrayList<>(1);
    private Scale prevXScale;
    private int prevPixelsPerDataPoint = -1;
    private int prevTraceDataSize;

    private int[] sorter;

    public TraceDataManager(ChartData traceData) {
        this.traceData = traceData;
        setConfig(new DataProcessingConfig());
    }

    public void appendData() {
        traceData.appendData();
        for (ChartData data : groupedDataList) {
            data.appendData();
        }
    }


    public void setConfig(DataProcessingConfig processingConfig) {
        this.processingConfig = processingConfig;
        int capacity = 1;
        if(isGroupIntervalsSpecified()) {
            capacity = processingConfig.getGroupingIntervals().length;
        }
        groupedDataList = new ArrayList<>(capacity);
        switch (processingConfig.getGroupingType()) {
            case EQUAL_POINTS_NUMBER:
                isEqualFrequencyGrouping = true;
                break;

            case EQUAL_INTERVALS:
                isEqualFrequencyGrouping = false;
                break;

            case AUTO:
                if (traceData.isColumnRegular(0)) {
                    isEqualFrequencyGrouping = true;
                } else {
                    isEqualFrequencyGrouping = false;
                }
                break;

            default:
                isEqualFrequencyGrouping = true;
                break;
        }
        prevXScale = null;
    }

    public Range getFullXMinMax() {
        if (traceData.columnCount() == 0) {
            return null;
        }
        return traceData.getColumnMinMax(0);
    }

    public int nearest(double xValue) {
        // "lazy" sorting solo when "nearest" is called
        ChartData data = traceData;
        if (processedData != null) {
            data = processedData;
        }
        if (sorter == null) {
            if (!data.isColumnIncreasing(0)) {
                sorter = data.sortedIndices(0);
            }
        }

        int nearest = data.bisect(0, xValue, sorter);

        if (nearest >= data.rowCount()) {
            nearest = data.rowCount() - 1;
        }

        int nearest_prev = nearest;
        if (nearest > 0) {
            nearest_prev = nearest - 1;
        }

        if (sorter != null) {
            nearest = sorter[nearest];
            nearest_prev = sorter[nearest_prev];
        }
        if (nearest != nearest_prev) {
            if (Math.abs(data.getValue(nearest_prev, 0) - xValue) < Math.abs(data.getValue(nearest, 0) - xValue)) {
                nearest = nearest_prev;
            }
        }

        return nearest;
    }

    private boolean isDataProcessingEnabled() {
        if ((!processingConfig.isCropEnabled() && !processingConfig.isGroupingEnabled())
                || traceData.rowCount() <= 1
                || traceData.columnCount() == 0
                || !traceData.isColumnIncreasing(0)) // if data not sorted (not increasing)
        {
            // No processing
            return false;
        }
        return true;
    }

    public ChartData getData(Scale xScale, int markSize) {
        if (!isDataProcessingEnabled()) { // No processing
            processedData = null;
            return traceData;
        }

        int pixelsPerDataPoint = 1;
        if (markSize > 0) {
            pixelsPerDataPoint = markSize;
        }
        if (!isProcessedDataOk(xScale, pixelsPerDataPoint)) {
            processedData = processData(xScale, pixelsPerDataPoint);
            prevXScale = xScale.copy();
            prevPixelsPerDataPoint = pixelsPerDataPoint;
            prevTraceDataSize = traceData.rowCount();
        }
        return processedData;
    }

    private boolean isProcessedDataOk(Scale xScale, int pixelsPerDataPoint) {
        if (processedData == null) {
            return false;
        }
        if (prevPixelsPerDataPoint != pixelsPerDataPoint) {
            return false;
        }
        if (prevXScale == null) {
            return false;
        }
        if (!prevXScale.getClass().equals(xScale.getClass())) {
            return false;
        }

        if (!Arrays.equals(prevXScale.getDomain(), xScale.getDomain())) {
            return false;
        }

        int prevLength = length(prevXScale);
        int length = length(xScale);
        if (prevLength == 0 || length == 0) {
            return false;
        }

        if (Math.abs(prevLength - length) * 100 / length > processingConfig.getGroupingStability()) {
            return false;
        }

        if (traceData.rowCount() != prevTraceDataSize) {
            double[] domain = xScale.getDomain();
            Double xMax = domain[domain.length - 1];
            if (traceData.getValue(prevTraceDataSize - 1, 0) < xMax) {
                return false;
            }
        }

        return true;
    }

    private int length(Scale scale) {
        double[] range = scale.getRange();
        return (int) Math.abs(range[range.length - 1] - range[0]);
    }


    public ChartData processData(Scale xScale, int pixelsPerDataPoint) {
        double[] range = xScale.getRange();
        double[] domain = xScale.getDomain();
        Double xMin = domain[0];
        Double xMax = domain[domain.length - 1];
        double xStart = range[0];
        double xEnd = range[range.length - 1];

        Range traceDataMinMax = traceData.getColumnMinMax(0);
        double dataStart = xScale.scale(traceDataMinMax.getMin());
        double dataEnd = xScale.scale(traceDataMinMax.getMax());

        double drawingAreaWidth = 0;
        Range intersection = Range.intersect(new Range(xStart, xEnd), new Range(dataStart, dataEnd));
        if (intersection != null) {
            drawingAreaWidth = intersection.length();
        }
        Range minMax = Range.intersect(traceDataMinMax, new Range(xMin, xMax));

        if (drawingAreaWidth < 1) {
            return traceData.view(0, 0);
        }

        double[] availableIntervals = processingConfig.getGroupingIntervals();

        double groupInterval = 0;
        double pointsInGroup = 1;
        int pointsInGroupRound = 1;
        int groupIntervalIndex = -1;

        boolean isGroupingEnabled = false;
        if (processingConfig.isGroupingEnabled()) {
            // calculate best grouping interval
            groupInterval = minMax.length() * pixelsPerDataPoint / drawingAreaWidth;;
            pointsInGroup = groupIntervalToPointsNumber(traceData, groupInterval);
            pointsInGroupRound = roundPointsNumber(pointsInGroup);

            if(pointsInGroupRound > 1) {
                // if available intervals are specified we choose the interval among the available ones
                if (isGroupIntervalsSpecified()) {
                    double precision = 0.1;
                    for (int i = 0; i < availableIntervals.length; i++) {
                        double interval_i = availableIntervals[i];
                        if (Math.abs(groupInterval - interval_i) < precision * interval_i || groupInterval < interval_i) {
                            groupInterval = availableIntervals[i];
                            groupIntervalIndex = i;
                            break;
                        }
                    }
                    if (groupIntervalIndex < 0) {
                        groupInterval = availableIntervals[availableIntervals.length - 1];
                        groupIntervalIndex = availableIntervals.length - 1;
                    }
                    pointsInGroup = groupIntervalToPointsNumber(traceData, groupInterval);
                    pointsInGroupRound = roundPointsNumber(pointsInGroup);
                } else {
                    // if intervals are not specified we adjust group interval to int number of points in group
                    groupInterval = pointsNumberToGroupInterval(traceData, pointsInGroupRound);
                }
                isGroupingEnabled = true;
            } else {
                if(processingConfig.isGroupingForced()) {
                    groupInterval = availableIntervals[0];
                    groupIntervalIndex = 0;
                    pointsInGroup = groupIntervalToPointsNumber(traceData, groupInterval);
                    pointsInGroupRound = roundPointsNumber(pointsInGroup);
                    isGroupingEnabled = true;
                }
            }
        }

        System.out.println(groupInterval+ "  " + pointsInGroup+" pointsInGroupRound "+pointsInGroupRound);


        boolean isAlreadyGrouped;
        int cropShoulder;
        if(isGroupingEnabled && processingConfig.isGroupAll()) {
            processedData = groupAll(groupInterval, groupIntervalIndex);
            cropShoulder = processingConfig.getCropShoulder();
            isAlreadyGrouped = true;
        } else {
            processedData = traceData;
            cropShoulder = processingConfig.getCropShoulder() * pointsInGroupRound;
            isAlreadyGrouped = false;
        }

        // if crop enabled
        if (processingConfig.isCropEnabled() && (traceDataMinMax.getMin() < xMin || traceDataMinMax.getMax() > xMax)) {
            int minIndex = 0;
            if (traceDataMinMax.getMin() < xMin) {
                minIndex = processedData.bisect(0, minMax.getMin(), null) - cropShoulder;
            }

            int maxIndex = processedData.rowCount() - 1;
            if (traceDataMinMax.getMax() > xMax) {
                maxIndex = processedData.bisect(0, minMax.getMax(), null) + cropShoulder;
            }
            if (minIndex < 0) {
                minIndex = 0;
            }
            if (maxIndex >= processedData.rowCount()) {
                maxIndex = processedData.rowCount() - 1;
            }

            processedData = processedData.view(minIndex, maxIndex - minIndex);
            // if data was not grouped before we group only visible data
            if (isGroupingEnabled  && !isAlreadyGrouped) {
                processedData = group(processedData, groupInterval);
                System.out.println("crop and group "+pointsInGroupRound);
            }
        } else { // if crop disabled
            // if grouping was not done before
            if (isGroupingEnabled  && !isAlreadyGrouped) {
                processedData = groupAll(groupInterval, groupIntervalIndex);
            }
        }
        return processedData;
    }


    private ChartData groupAll(double groupInterval, int groupIntervalIndex) {
        if(groupIntervalIndex < 0) {
            return groupAllIfIntervalsNotSpecified(groupInterval);
        } else {
            return groupAllIfIntervalsSpecified(groupIntervalIndex);
        }
    }

    private ChartData groupAllIfIntervalsNotSpecified(double groupInterval) {
        ChartData groupedDataNew = null;
        if(groupedDataList.size() > 0 &&  processingConfig.getReGroupingStep() > 1) {
            // we try to use already grouped data
            ChartData groupedData = groupedDataList.get(0);
            double groupedPointsInGroup = groupIntervalToPointsNumber(groupedData, groupInterval);
            int groupedPointsInGroupRound = roundPointsNumber(groupedPointsInGroup);

            boolean isNextStepGrouping = groupedPointsInGroupRound >= processingConfig.getReGroupingStep();
            boolean isPrevStepGrouping = groupedPointsInGroup < 1.0 / processingConfig.getReGroupingStep();

            if(isNextStepGrouping) {
                if(isEqualFrequencyGrouping ) {
                    // we use already grouped data for further grouping
                    groupedDataNew = groupedData.resampleByEqualFrequency(groupedPointsInGroupRound);
                    groupedDataNew.cache();
                    // force "lazy" grouping
                    int rowCount = groupedDataNew.rowCount();
                    for (int i = 0; i < groupedDataNew.columnCount(); i++) {
                        groupedDataNew.getValue(rowCount - 1, i);
                    }
                    // Very important to clean memory!!!
                    groupedData.disableCaching();
                    System.out.println("regroup  " + groupedPointsInGroupRound);
                } else {
                    double nextStepInterval = pointsNumberToGroupInterval(groupedData, groupedPointsInGroupRound);
                    groupedDataNew = traceData.resampleByEqualInterval(0, nextStepInterval);
                    groupedDataNew.cache();
                }
            }

            if(!isNextStepGrouping && !isPrevStepGrouping) {
                // no resample (we use already grouped data as it is)
                System.out.println("the same data ");
                groupedDataNew = groupedDataList.get(0);
            }
        }

        if(groupedDataNew == null) {
            groupedDataNew = group(traceData, groupInterval);
        }
        groupedDataList.clear();
        groupedDataList.add(groupedDataNew);
        return groupedDataNew;
    }


    private ChartData groupAllIfIntervalsSpecified(int groupIntervalIndex) {
        double[] groupIntervals = processingConfig.getGroupingIntervals();
        for (int i = groupedDataList.size(); i <= groupIntervalIndex; i++) {
            ChartData groupedData = null;
            if (i > 0 && isEqualFrequencyGrouping) { // group by equal points number
                int pointsInGroup = roundPointsNumber(groupIntervalToPointsNumber(traceData, groupIntervals[i]));
                if(pointsInGroup > 1) {
                    int prevPointsInGroup = roundPointsNumber(groupIntervalToPointsNumber(traceData, groupIntervals[i - 1]));
                    if(pointsInGroup % prevPointsInGroup == 0) {
                        int pointsRatio = pointsInGroup / prevPointsInGroup;
                        if(pointsRatio > 1) {
                            // regroup on the base of already grouped data
                            groupedData = groupedDataList.get(i - 1).resampleByEqualFrequency(pointsRatio);
                            groupedData.cache();
                            System.out.println("regroup  " + pointsRatio);
                        } else if(pointsRatio == 1) {
                            // use already grouped data as it is
                            groupedData = groupedDataList.get(i - 1);
                            System.out.println("the same data ");
                        }
                    }
                }
            }
            if(groupedData == null) {
                groupedData = group(traceData, groupIntervals[i]);
            }
            groupedDataList.add(groupedData);
        }
        return groupedDataList.get(groupIntervalIndex);
    }

    private ChartData group(ChartData data, double groupInterval) {
        ChartData groupedData;
        if (isEqualFrequencyGrouping) { // group by equal points number
            int pointNumber = roundPointsNumber(groupIntervalToPointsNumber(data, groupInterval));
            if(pointNumber > 1) {
                groupedData = data.resampleByEqualFrequency(pointNumber);
                groupedData.cache();
            } else {
                groupedData = data;
            }
        } else {
            groupedData = data.resampleByEqualInterval(0, groupInterval);
            groupedData.cache();
        }
        System.out.println("new group  " + groupInterval + "  " + roundPointsNumber(groupIntervalToPointsNumber(data, groupInterval)));

        return groupedData;

    }

    public double getBestExtent(int drawingAreaWidth, int markSize) {
        if (traceData.rowCount() > 1) {
            double traceExtent = getDataAvgStep(traceData) * drawingAreaWidth;
            if (markSize > 0) {
                traceExtent = traceExtent / markSize;
            }
            if(processingConfig.isGroupingForced() && isGroupIntervalsSpecified()) {
                double groupInterval = processingConfig.getGroupingIntervals()[0];
                double pointsInGroup = groupIntervalToPointsNumber(traceData, groupInterval);
                if(roundPointsNumber(pointsInGroup) > 1) {
                    traceExtent *= pointsInGroup;
                }
            }
            return traceExtent;
        }
        return 0;
    }


    private boolean isGroupIntervalsSpecified() {
        if(processingConfig.getGroupingIntervals() != null && processingConfig.getGroupingIntervals().length > 0) {
            return true;
        }
        return false;
    }

    private double pointsNumberToGroupInterval(ChartData data, double pointsInGroup) {
        return pointsInGroup * getDataAvgStep(data);
    }

    private double groupIntervalToPointsNumber(ChartData data, double groupInterval) {
       return groupInterval / getDataAvgStep(data);
    }

    private int roundPointsNumber(double points) {
        double precision = 0.2;
        if(points < 1 + precision) {
           return 1;
        }

        int points_ceil = (int) Math.ceil(points);
        if (points_ceil - points > 1 - precision) {
            points_ceil--;
        }
        return points_ceil;
    }

    double getDataAvgStep(ChartData data) {
        int dataSize = data.rowCount();
        return (data.getValue(dataSize - 1, 0) - data.getValue(0, 0)) / (dataSize - 1);
    }
}
