package com.biorecorder.basechart.grouping;


public class LongGroupSum extends LongGroupFunction {
    private long sum;

    @Override
    public void reset() {
        super.reset();
        sum = 0;
    }

    @Override
    protected void add1(long value) {
        super.add1(value);
        sum += value;
    }

    @Override
    protected long[] groupedValue1() {
        long[] groupedValues = {sum};
        return groupedValues;
    }
}