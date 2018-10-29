package com.biorecorder.basechart.data;

import com.biorecorder.util.lists.SeriesUtil;
import com.biorecorder.basechart.Range;
import com.biorecorder.util.lists.IntArrayList;
import com.biorecorder.basechart.grouping.GroupingApproximation;
import com.biorecorder.basechart.grouping.IntGroupingFunction;
import com.biorecorder.util.series.IntSeries;
import com.biorecorder.util.series.LongSeries;

import java.util.List;

/**
 * Created by galafit on 27/9/17.
 */
class IntColumn extends NumberColumn {
    private final IntSeries series;
    private SeriesRangeViewer seriesViewer;

    public IntColumn(IntSeries series) {
        this.series = series;
        this.seriesViewer = new SeriesRangeViewer();
    }

    public IntColumn(int[] data) {
        this(new IntSeries() {
            @Override
            public long size() {
                return data.length;
            }

            @Override
            public int get(long index) {
                return data[(int) index];
            }
        });
    }

    public IntColumn(List<Integer> data) {
        this(new IntSeries() {
            @Override
            public long size() {
                return data.size();
            }

            @Override
            public int get(long index) {
                return data.get((int) index);
            }
        });
    }

    @Override
    public void enableCaching(boolean isLastElementCached) {
        SeriesCachingRangeViewer seriesViewer1 = new SeriesCachingRangeViewer(isLastElementCached);
        seriesViewer1.setViewRange(seriesViewer.getRangeStart(), seriesViewer.getRangeLength());
        seriesViewer = seriesViewer1;
    }

    @Override
    public void enableCaching(boolean isLastElementCached, NumberColumn column) {
        SeriesCachingRangeViewer seriesViewer1 = new SeriesCachingRangeViewer(isLastElementCached);
        seriesViewer1.setViewRange(seriesViewer.getRangeStart(), seriesViewer.getRangeLength());
        seriesViewer1.cache(column);
        seriesViewer = seriesViewer1;
    }

    @Override
    public void disableCaching() {
        SeriesRangeViewer seriesViewer1 = new SeriesRangeViewer();
        seriesViewer1.setViewRange(seriesViewer.getRangeStart(), seriesViewer.getRangeLength());
        seriesViewer = seriesViewer1;
    }

    @Override
    public void clear() {
        if(seriesViewer instanceof SeriesCachingRangeViewer) {
            ((SeriesCachingRangeViewer) seriesViewer).clear();
        }
    }

    @Override
    public NumberColumn[] group(LongSeries groupIndexes) {
        return new GroupingManager(groupingType, groupIndexes).groupedColumns();
    }

    @Override
    public void setViewRange(long rangeStart, long rangeLength) {
        seriesViewer.setViewRange(rangeStart, rangeLength);
    }

    @Override
    public long size() {
        return seriesViewer.size();
    }

    @Override
    public double value(long index) {
        return seriesViewer.get(index);
    }

    @Override
    public Range extremes(long from, long length) {
        if(length == 0){
            return null;
        }
        if (length > Integer.MAX_VALUE) {
            String errorMessage = "Extremes can not be find if length > Integer.MAX_VALUE. Length = " + length;
            throw new IllegalArgumentException(errorMessage);
        }

        // invoke data.get(i) can be expensive in the case data is grouped data
        int dataItem = seriesViewer.get(from); //
        int min = dataItem;
        int max = dataItem;
        for (long i = from + 1; i < from + length ; i++) {
            dataItem = seriesViewer.get(i);
            min = (int)Math.min(min, dataItem);
            max = (int)Math.max(max, dataItem);
        }
        return new Range(min, max);
    }

    @Override
    public long binarySearch(double value, long from, int length) {
        if (length >Integer.MAX_VALUE) {
            String errorMessage = "Binary search can not be done if length > Integer.MAX_VALUE. Length = " + length;
            throw new IllegalArgumentException(errorMessage);
        }
        return SeriesUtil.upperBound(seriesViewer, (int) value, 0, (int) length);

    }

    @Override
    public long upperBound(double value, long from, long length) {
        if (length > Integer.MAX_VALUE) {
            String errorMessage = "Upper bound binary search not be done if length > Integer.MAX_VALUE. length = " + length;
            throw new IllegalArgumentException(errorMessage);
        }
        return SeriesUtil.upperBound(seriesViewer, (int) value, 0, (int) length);
    }

    @Override
    public long lowerBound(double value, long from, long length) {
        if (length > Integer.MAX_VALUE) {
            String errorMessage = "Lower bound binary search not be done if length > Integer.MAX_VALUE. Length = " + length;
            throw new IllegalArgumentException(errorMessage);
        }
        return SeriesUtil.lowerBound(seriesViewer, (int) value, from, (int) length);
    }

    @Override
    public NumberColumn copy() {
        IntColumn newColumn = new IntColumn(series);
        newColumn.name = name;
        newColumn.groupingType = groupingType;
        return newColumn;
    }


    class GroupingManager {
        private GroupingApproximation groupingApproximation;
        private LongSeries groupStartIndexes;
        private long lastGroupValueStart = -1;
        private long lastGroupValueLength;

        private final IntGroupingFunction groupingFunction;

        public GroupingManager(GroupingApproximation groupingApproximation, LongSeries groupStartIndexes) {
            this.groupingApproximation = groupingApproximation;
            groupingFunction = (IntGroupingFunction) groupingApproximation.getGroupingFunction("int");
            this.groupStartIndexes = groupStartIndexes;
        }

        private long groupsCount() {
            return groupStartIndexes.size() - 1;
        }

        private int[] getGroupValues(long groupIndex) {
            if(lastGroupValueStart != groupStartIndexes.get(groupIndex)) {
               groupingFunction.reset();
               lastGroupValueLength = 0;
            }
            int[] groupedValues = groupingFunction.addToGroup(seriesViewer, groupStartIndexes.get(groupIndex) + lastGroupValueLength, groupStartIndexes.get(groupIndex + 1) - groupStartIndexes.get(groupIndex) - lastGroupValueLength);

            lastGroupValueStart = groupStartIndexes.get(groupIndex);
            lastGroupValueLength = groupStartIndexes.get(groupIndex + 1) - groupStartIndexes.get(groupIndex);
            return groupedValues;
        }

        public NumberColumn[] groupedColumns() {
            NumberColumn[] resultantColumns = new NumberColumn[groupingApproximation.getDimension()];
            for (int i = 0; i < resultantColumns.length; i++) {
                final int seriesNumber = i;
                IntSeries groupedSeries = new IntSeries() {
                    @Override
                    public long size() {
                        return groupsCount();
                    }

                    @Override
                    public int get(long index) {
                        return getGroupValues(index)[seriesNumber];
                    }
                };
                resultantColumns[i] = new IntColumn(groupedSeries) {
                    @Override
                    public void clear() {
                        super.clear();
                        groupingFunction.reset();
                    }

                    @Override
                    public void enableCaching(boolean isLastElementCached, NumberColumn column) {
                        super.enableCaching(isLastElementCached, column);
                        groupingFunction.reset();
                    }
                };
                resultantColumns[i].setName(name);
                resultantColumns[i].setGroupingType(groupingApproximation);
            }
            if (resultantColumns.length == 2) { // MinMAx
                resultantColumns[0].setGroupingType(GroupingApproximation.MIN);
                resultantColumns[1].setGroupingType(GroupingApproximation.MAX);
            }

            return resultantColumns;
        }
    }

    class  SeriesRangeViewer implements IntSeries {
        private long rangeStart = 0;
        private long rangeLength = -1;

        public void setViewRange(long rangeStart1, long rangeLength1) {
            rangeStart = rangeStart1;
            rangeLength = rangeLength1;
            if (rangeStart < 0) {
                rangeStart = 0;
            }
            if (rangeStart >= series.size()) {
                rangeLength = 0;
            }
            if (rangeLength > series.size() - rangeStart) {
                rangeLength = series.size() - rangeStart;
            }
        }

        public long getRangeStart() {
            return rangeStart;
        }

        public long getRangeLength() {
            return rangeLength;
        }

        @Override
        public long size() {
            if (rangeLength < 0) {
                return series.size() - rangeStart;
            }
            return rangeLength;
        }

        @Override
        public int get(long index) {
            return series.get(index + rangeStart);
        }
    }


    class SeriesCachingRangeViewer extends SeriesRangeViewer {
        private IntArrayList cache;
        private boolean isLastElementCached;

        public SeriesCachingRangeViewer(boolean isLastElementCached) {
            this.isLastElementCached = isLastElementCached;
            cache = new IntArrayList();
        }

        @Override
        public int get(long index) {
            if (!isLastElementCached && index == size() - 1) {
                return super.get(index);
            }

            if (index >= cache.size()) {
                for (long i = cache.size(); i <= index; i++) {
                    cache.add(super.get(index));
                }
            }

            return cache.get(index);
        }

        @Override
        public void setViewRange(long rangeStart1, long rangeLength1) {
            super.setViewRange(rangeStart1, rangeLength1);
            cache.clear();
        }

        public void clear() {
            cache.clear();
        }

        public void cache(NumberColumn column) {
            for (int i = 0; i < column.size() - 1; i++) {
                cache.add((int) column.value(i));
            }
            if (isLastElementCached) {
                cache.add((int) column.value(column.size() - 1));
            }
        }
    }
}