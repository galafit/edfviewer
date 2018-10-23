package com.biorecorder.basechart.grouping;


public class FloatGroupingAvg extends FloatGroupingFunction {
    private float sum;

    @Override
    public void reset() {
        super.reset();
        sum = 0;
    }

    @Override
    protected void add1(float value) {
        super.add1(value);
        sum += value;
    }

    @Override
    protected float[] groupedValue1() {
        float[] groupedValues = {sum / count};
        return groupedValues;
    }
}