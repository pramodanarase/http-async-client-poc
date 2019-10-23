package org.async.metric;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetricStore {

    private static Map<String,SupportabilityMetric> metricMap = new ConcurrentHashMap<>();

    private static boolean enableSupportabilityMetric = false;

    //metric initilization
    public static void enableSupportabilityMetric() {
        enableSupportabilityMetric = true;
        metricMap.put(SupportabilityMetricConstant.PROFILE_QUEUE_SIZE,new SupportabilityMetric(SupportabilityMetricConstant.PROFILE_QUEUE_SIZE,-1,"LONG","number","COUNT",0));
        metricMap.put(SupportabilityMetricConstant.NUMBER_OF_API_CALL,new SupportabilityMetric(SupportabilityMetricConstant.NUMBER_OF_API_CALL,-1,"LONG","number","SUM",0));
        metricMap.put(SupportabilityMetricConstant.API_TIME,new SupportabilityMetric(SupportabilityMetricConstant.API_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.REQUEST_METRICQUEUE_SIZE,new SupportabilityMetric(SupportabilityMetricConstant.REQUEST_METRICQUEUE_SIZE,-1,"LONG","number","COUNT",0));
        metricMap.put(SupportabilityMetricConstant.PROFILE_TIME,new SupportabilityMetric(SupportabilityMetricConstant.PROFILE_TIME,-1,"LONG","ms","AVG",0));

        metricMap.put(SupportabilityMetricConstant.API_STORE_TIME,new SupportabilityMetric(SupportabilityMetricConstant.API_STORE_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.DATA_STORE_TIME,new SupportabilityMetric(SupportabilityMetricConstant.DATA_STORE_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.DEPENDENT_DATA_DELETE_TIME,new SupportabilityMetric(SupportabilityMetricConstant.DEPENDENT_DATA_DELETE_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.UPDATE_DATA_RUN_TIME,new SupportabilityMetric(SupportabilityMetricConstant.UPDATE_DATA_RUN_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.PROCESS_METRICDATA_TIME,new SupportabilityMetric(SupportabilityMetricConstant.PROCESS_METRICDATA_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.PUBLISH_METRICDATA_TIME,new SupportabilityMetric(SupportabilityMetricConstant.PUBLISH_METRICDATA_TIME,-1,"LONG","ms","AVG",0));
        metricMap.put(SupportabilityMetricConstant.PUBLISH_METRICQUEUE_SIZE,new SupportabilityMetric(SupportabilityMetricConstant.PUBLISH_METRICQUEUE_SIZE,-1,"LONG","number","COUNT",0));
        metricMap.put(SupportabilityMetricConstant.INTERVAL_MISS_HIT,new SupportabilityMetric(SupportabilityMetricConstant.INTERVAL_MISS_HIT,-1,"LONG","number","SUM",0));
        metricMap.put(SupportabilityMetricConstant.DB_CONNECTION_COUNT,new SupportabilityMetric(SupportabilityMetricConstant.DB_CONNECTION_COUNT,-1,"LONG","number","COUNT",0));

    }

    public static Map<String,SupportabilityMetric> getMetricMap(){
        return metricMap;
    }

    public static void putMetric(String metrickey, long metricValue){
        if(enableSupportabilityMetric) {
            SupportabilityMetric metric = metricMap.get(metrickey);

            int dataPoints = metric.getDataPointCount();
            metric.setDataPointCount(++dataPoints);

            if(metric.getValue().get()==-1) metric.setValue(0);

            String stat = metric.getStatistic();
            switch (stat) {
                case "SUM":
                    sumMetric(metric,metricValue );
                    break;
                case "COUNT":
                    countMetric(metric,metricValue);
                    break;
                default:
                case "AVG":
                    avgMetric(metric, metricValue);
                    break;
            }
        }
    }

    private static void avgMetric(SupportabilityMetric metric, long metricValue) {
        long avg = metric.getValue().get();
        int datapoint = metric.getDataPointCount();
        avg -=avg/datapoint;
        avg+=metricValue /datapoint;
        metric.setValue(avg);
    }

    private static void sumMetric(SupportabilityMetric metric, long metricValue) {
        metric.setValue(metric.getValue().get() + metricValue);
    }

    private static void countMetric(SupportabilityMetric metric, long metricValue) {
        metric.setValue(metricValue);
    }

}
