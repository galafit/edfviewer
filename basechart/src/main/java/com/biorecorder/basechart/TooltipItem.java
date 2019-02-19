package com.biorecorder.basechart;


import com.biorecorder.basechart.graphics.BColor;

/**
 * Created by galafit on 6/8/17.
 */
public class TooltipItem {
    private String label;
    private String value;
    private BColor markColor;

    public TooltipItem(String label, String value, BColor markColor) {
        this.label = label;
        this.value = value;
        this.markColor = markColor;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BColor getMarkColor() {
        return markColor;
    }

    public void setMarkColor(BColor markColor) {
        this.markColor = markColor;
    }
}