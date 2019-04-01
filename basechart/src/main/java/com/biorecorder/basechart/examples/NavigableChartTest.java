package com.biorecorder.basechart.examples;

import com.biorecorder.basechart.*;
import com.biorecorder.basechart.scales.LinearScale;
import com.biorecorder.basechart.themes.DarkTheme;
import com.biorecorder.basechart.themes.WhiteTheme;
import com.biorecorder.data.frame.SquareFunction;
import com.biorecorder.data.list.IntArrayList;
import com.biorecorder.basechart.swing.ChartPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by galafit on 27/9/18.
 */
public class NavigableChartTest extends JFrame{
    IntArrayList yData;
    IntArrayList xData;
    ChartPanel chartPanel;
    NavigableChart chart;
    XYData xyData;

    public NavigableChartTest() {
        int width = 400;
        int height = 500;

        setTitle("Test chart");

        yData = new IntArrayList();
        xData = new IntArrayList();

        for (int i = 0; i < 200; i++) {
            yData.add(i);
        }


        for (int i = 0; i < 200; i++) {
            xData.add(i);
        }

        xyData = new XYData(0, 1);
        //xyData.addColumn(xData);
        xyData.addColumn(yData);
       // xyData.addColumn(new SquareFunction(), 0);

        chart = new NavigableChart(new WhiteTheme(true).getNavigableChartConfig());

        DataProcessingConfig navigatorProcessing = new DataProcessingConfig();
        double[] groupingIntervals = {20, 40};
        navigatorProcessing.setGroupingIntervals(groupingIntervals);
        navigatorProcessing.setGroupingForced(true);
        chart = new NavigableChart(new WhiteTheme(true).getNavigableChartConfig(), new LinearScale(), new LinearScale(), new DataProcessingConfig(), navigatorProcessing);

        chart.addChartTrace(new LineTrace(xyData), true , false, false);

        chart.addNavigatorTrace( new LineTrace(xyData), true);

        chartPanel = new ChartPanel(chart);

        chartPanel.setPreferredSize(new Dimension(width, height));
        add(chartPanel, BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addKeyListener(chartPanel);
        setLocationRelativeTo(null);
        setVisible(true);

        Thread t1 = new Thread(new Runnable() {
            int interval = 2000;
            @Override
            public void run() {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                NavigableChartConfig config = new DarkTheme().getNavigableChartConfig();
                config.setGap(20);
                chart.setConfig(config, true);
                chartPanel.repaint();

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                chart.setChartYTitle(1, "title");

                xyData.addColumn(new SquareFunction(), 0);
                chart.addChartTrace(new LineTrace(xyData), true);
                chartPanel.repaint();
            }
        });
        t1.start();

        Thread t = new Thread(new Runnable() {
            int interval = 1000;
            @Override
            public void run() {
                for (int count = 0; count < 10; count++) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    int yDataLast = yData.get(yData.size() - 1);
                    int xDataLast = xData.get(xData.size() - 1);

                    for (int i = 1; i <= 100; i++) {
                        yData.add(i + yDataLast);
                        xData.add(i + xDataLast);

                    }
                    System.out.println("\ndata size: "+yData.size());

                    chart.appendData();
                    chartPanel.repaint();
                }
            }
        });
     //  t.start();
    }


    public static void main(String[] args) {
        NavigableChartTest chartTest = new NavigableChartTest();
    }
}
