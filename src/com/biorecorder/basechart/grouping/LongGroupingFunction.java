package com.biorecorder.basechart.grouping;

import com.biorecorder.util.series.LongSeries;

/**
 * Created by galafit on 22/10/18.
 */
public abstract class LongGroupingFunction {
    protected long count;

    public long[] addToGroup(LongSeries series, long from, long length) {
        for (int i = 0; i < length; i++) {
           add1(series.get(from + i));
        }
        return groupedValue();
    }

    public void reset() {
        count = 0;
    }

    public long[] groupedValue() {
        if(count == 0) {
            String errMsg = "No elements was added to group. Grouping function can not be calculated.";
            throw new IllegalStateException(errMsg);
        }
        return groupedValue1();
    }

    protected void add1(long value) {
       count++;
    }

    protected abstract long[] groupedValue1();
}