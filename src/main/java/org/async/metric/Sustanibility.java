package org.async.metric;


import org.async.HttpAsyn;
import org.async.ProfileTask;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.async.metric.SupportabilityMetricConstant.*;


public class Sustanibility implements Runnable {

    private static Sustanibility publishSupportabilityMetric = new Sustanibility();
    public static ScheduledThreadPoolExecutor executor = null;

    private static final String path = "Agent Stats|Sustainability|Restmon:";


    public static void initPublisher( ){
        if(executor == null) {
            executor = new ScheduledThreadPoolExecutor(1);
            executor.scheduleAtFixedRate(publishSupportabilityMetric, 500, 7000, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void run(){
        //emmit pending profile task metric
        MetricStore.putMetric(PROFILE_QUEUE_SIZE, HttpAsyn.getPendingTaskCount());
        //emmit request queue size
        MetricStore.putMetric(REQUEST_METRICQUEUE_SIZE, ProfileTask.getRequestExecutorQueuSize());

      //  MetricStore.putMetric(REQUEST_METRICQUEUE_SIZE, ExecuteUrlAsync.asynchttpclient.getConfig().);


        for(SupportabilityMetric m : MetricStore.getMetricMap().values()){
            if(m.getValue().get()!= -1){
                //SendMetricsToAPM.reportMetric(path+m.getName(),getMetricType(m.getStatistic()), ""+m.getValue().get());
                System.out.println(m.getDataPointCount()+ ", "+ m.getName() + ", " + m.getValue());
            }
        }
        //reset metrics
        resetMetrics();
    }

    private String getMetricType(String statistic){
        String metricType="LongCounter";
        switch (statistic){
            case "SUM" :
                metricType = "PerIntervalCounter";
                break;
            case "AVG" :
                metricType = "LongAverage";
                break;
            case "COUNT" :
                metricType = "LongCounter";
                break;
        }
        return metricType;
    }

    private static void resetMetrics(){

        for(SupportabilityMetric m : MetricStore.getMetricMap().values()){
            m.setValue(-1);
            m.setDataPointCount(0);
        }
    }
}

