package com.biorecorder.basechart;

import com.biorecorder.basechart.config.DataProcessingConfig;
import com.biorecorder.basechart.data.Data;
import com.biorecorder.basechart.data.DataSeries;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by galafit on 18/7/18.
 */
public class DataManager {
    private List<TraceDataManager> traceDataManagers = new ArrayList<>();
    private DataProcessingConfig processingConfig;

    public DataManager(DataProcessingConfig processingConfig) {
        this.processingConfig = processingConfig;
    }

    public void addTrace(DataSeries traceData, int pixelsInDataPoint) {
        traceDataManagers.add(new TraceDataManager(traceData, processingConfig, pixelsInDataPoint));
    }

    public DataSeries getOriginalTraceData(int traceNumber) {
        return traceDataManagers.get(traceNumber).getOriginalData();
    }

    public DataSeries getProcessedTraceData(int traceNumber, Double min, Double max) {
       return traceDataManagers.get(traceNumber).getProcessedData(min, max);
    }
}