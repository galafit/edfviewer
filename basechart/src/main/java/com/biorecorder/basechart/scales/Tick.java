package com.biorecorder.basechart.scales;

import com.biorecorder.basechart.utils.NormalizedNumber;

/**
 * Created by galafit on 5/9/17.
 */
public class Tick {
    private NormalizedNumber value;
    private String label;

    public Tick(NormalizedNumber tickValue, String tickLabel) {
        this.value = tickValue;
        this.label = tickLabel;
    }

    public double getValue() {
        return value.value();
    }

    public String getLabel() {
        return label;
    }
}