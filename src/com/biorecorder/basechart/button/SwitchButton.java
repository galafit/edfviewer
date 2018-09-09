package com.biorecorder.basechart.button;

import com.biorecorder.basechart.*;
import com.biorecorder.basechart.graphics.*;

/**
 * Created by galafit on 18/12/17.
 */
public class SwitchButton {
    private ButtonModel model = new ButtonModel();
    private BColor color = BColor.BLACK;
    private String label = "";
    private BColor backgroundColor = BColor.LIGHT_GRAY;
    private TextStyle textStyle = new TextStyle(TextStyle.DEFAULT, TextStyle.NORMAL, 12);
    private Margin margin = new Margin((int)(textStyle.getSize() * 0.2),
            (int)(textStyle.getSize() * 0.2),
            (int)(textStyle.getSize() * 0.2),
            (int)(textStyle.getSize() * 0.2));

    private BRectangle bounds;

    public SwitchButton(BColor color, String label) {
        this.color = color;
        this.label = label;
    }


    public void switchState() {
        if(model.isSelected()) {
            model.setSelected(false);
        } else {
            model.setSelected(true);
        }
    }

    public boolean contains(int x, int y) {
        if(bounds != null && bounds.contains(x, y)) {
            return true;
        }
        return false;
    }

    public void addListener(StateListener listener) {
        model.addListener(listener);
    }

    public ButtonModel getModel() {
        return model;
    }

    public void setLocation(int x, int y, BCanvas canvas) {
        if(bounds == null) {
            createBounds(canvas);
        }
        bounds.x = x;
        bounds.y = y;
    }

    public void moveLocation(int dx, int dy) {
        if(bounds == null) {
            return;
        }
        bounds.x += dx;
        bounds.y += dy;
    }


    public BRectangle getBounds(BCanvas canvas) {
        if(bounds == null) {
            createBounds(canvas);
        }
        return bounds;
    }

    private void createBounds(BCanvas canvas) {
        TextMetric tm = canvas.getTextMetric(textStyle);
        bounds = new BRectangle(0, 0, getItemWidth(tm), getItemHeight(tm));
    }


    public void draw(BCanvas canvas) {
        if(bounds == null) {
            createBounds(canvas);
        }
        // draw backgroundColor
        canvas.setColor(backgroundColor);
        canvas.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        canvas.setColor(color);

        // draw item
        TextMetric tm = canvas.getTextMetric(textStyle);
        int x = bounds.x + margin.left();
        int y = bounds.y + margin.top() + tm.ascent();
        canvas.drawString(label, x, y);

        if(model.isSelected()) {
            // draw border
            canvas.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height);
            // draw selection marker
            x = bounds.x + margin.left() + tm.stringWidth(label) + getCheckMarkPadding();
            y = bounds.y + bounds.height/2;

            int x1 = x + getCheckMarkSize()/2;
            int y1 = bounds.y + bounds.height - margin.bottom();

            int x2 = x + getCheckMarkSize();
            int y2 = bounds.y + margin.top();

            canvas.drawLine(x, y, x1, y1);
            canvas.drawLine(x1, y1, x2, y2);
        }

    }

    private int getItemWidth(TextMetric tm) {
        return tm.stringWidth(label) + getCheckMarkSize() + getCheckMarkPadding()
                + margin.left() + margin.right();

    }

    private int getItemHeight(TextMetric tm) {
        return tm.height() + margin.top() + margin.bottom();

    }

    private int getCheckMarkSize() {
        return (int) (textStyle.getSize() * 0.8);
    }

    private int getCheckMarkPadding() {
        return (int) (textStyle.getSize() * 0.5);
    }

    public void setBackgroundColor(BColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle;
        bounds = null;
    }

    public void setMargin(Margin margin) {
        this.margin = margin;
        bounds = null;
    }

    public void setColor(BColor color) {
        this.color = color;
    }
}
