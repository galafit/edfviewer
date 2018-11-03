package com.biorecorder.basechart.grouping;


public class IntGroupOpen extends IntGroupFunction {
    private int first;

    @Override
    protected void add1(int value) {
        if(count == 0) {
            first = value;
        }
        count++;
    }

    @Override
    protected int[] groupedValue1() {
        int[] groupedValues = {first};
        return groupedValues;
    }
}