package com.biorecorder.basechart.scroll;


import com.biorecorder.basechart.graphics.BColor;

/**
 * Created by galafit on 1/10/17.
 */
public class ScrollConfig {
    private BColor color = BColor.RED;
    private int extraSpace = 5; //px
    private int borderWidth = 2; // px

    public ScrollConfig() {
    }

    public ScrollConfig(ScrollConfig scrollConfig) {
        color = scrollConfig.color;
        extraSpace = scrollConfig.extraSpace;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    public void setColor(BColor color) {
        this.color = color;
    }

    public void setExtraSpace(int activeExtraSpace) {
        this.extraSpace = activeExtraSpace;
    }

    public BColor getColor() {
        return color;
    }

    public int getExtraSpace() {
        return extraSpace;
    }

}