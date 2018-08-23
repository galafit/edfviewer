package com.biorecorder.basechart.data;

import com.biorecorder.basechart.Range;
import com.biorecorder.basechart.grouping.GroupingType;
import com.biorecorder.basechart.series.LongSeries;

/**
 * Created by galafit on 17/9/17.
 */
public abstract class NumberColumn {
    protected String name;
    protected GroupingType groupingType = GroupingType.AVG;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GroupingType getGroupingType() {
        return groupingType;
    }

    public void setGroupingType(GroupingType groupingType) {
        this.groupingType = groupingType;
    }

    public abstract long size();
    public abstract double value(long index);

    // if length == -1 real size will be used
    public abstract void setViewRange(long from, long length);

    public abstract Range extremes(long length);
    public abstract long upperBound(double value, long length);
    public abstract long lowerBound(double value, long length);
    public abstract void enableCaching(boolean isLastElementCacheable);
    public abstract void disableCaching();
    public abstract void clear();

    public abstract NumberColumn[] group(LongSeries groupIndexes);

    public abstract NumberColumn copy();
    public abstract void cache(NumberColumn column);
}
