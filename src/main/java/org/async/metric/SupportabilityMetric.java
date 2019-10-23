package org.async.metric;

import java.util.concurrent.atomic.AtomicLong;


public class SupportabilityMetric {
    private String name;
    //metric value
    private AtomicLong value = new AtomicLong();
    //metric type - LONG, INT
    private String type;
    //metric unit
    private String unit;
    //statistic type- SUM, AVG, COOUNT
    private String statistic;
    //data point count till it reset
    private int dataPointCount;

    public SupportabilityMetric(String name, long value, String type, String unit, String statistic, int dataPointCount) {
        this.name = name;
        this.value.set(value);
        this.type = type;
        this.unit = unit;
        this.statistic = statistic;
        this.dataPointCount = dataPointCount;
    }


    public String getName() {
        return name;
    }

    public AtomicLong getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public String getStatistic() {
        return statistic;
    }

    public void setValue(long value) {
        this.value.set(value);
    }

    public int getDataPointCount() {
        return dataPointCount;
    }

    public void setDataPointCount(int dataPointCount) {
        this.dataPointCount = dataPointCount;
    }


}
