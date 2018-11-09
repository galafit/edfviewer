package com.biorecorder.basecharts;

import com.biorecorder.basecharts.graphics.BColor;
import com.biorecorder.basecharts.graphics.TextStyle;

/**
 * Created by galafit on 31/8/18.
 */
public class TitleConfig {
    private TextStyle textStyle = new TextStyle(TextStyle.DEFAULT, TextStyle.NORMAL, 14);
    private BColor textColor = BColor.BLACK;
    private Insets margin = new Insets(textStyle.getSize());

    public TitleConfig() {
    }

    public TitleConfig(TitleConfig titleConfig) {
        textStyle = titleConfig.textStyle;
        textColor = titleConfig.textColor;
        margin = titleConfig.margin;
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle;
    }

    public BColor getTextColor() {
        return textColor;
    }

    public void setTextColor(BColor textColor) {
        this.textColor = textColor;
    }

    public Insets getMargin() {
        return margin;
    }

    public void setMargin(Insets margin) {
        this.margin = margin;
    }
}