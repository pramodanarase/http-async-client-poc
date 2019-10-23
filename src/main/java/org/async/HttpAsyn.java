package org.async;


import org.async.metric.MetricStore;
import org.async.metric.Sustanibility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpAsyn {

    private static int maxUrls;
    private static int profileSize;
    private static int interval;
    private static int maxTcpConnection;
    private static int networkLatency;
    private static int profileThread;
    private static int reqThread;

    public static boolean singleRun = true;
    public static int type = 0;

    //schedular
    public static ScheduledThreadPoolExecutor scheduler;
    //10 - profile thread
    public static ThreadPoolExecutor executor;

    public static void main(String[] args) {

        try (InputStream input = HttpAsyn.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            maxUrls = Integer.parseInt(prop.getProperty("http.async.maxUrl"));
            profileSize = Integer.parseInt(prop.getProperty("http.async.profileSize"));
            maxTcpConnection = Integer.parseInt(prop.getProperty("http.async.maxTcpConnection"));
            interval = Integer.parseInt(prop.getProperty("http.async.interval"));
            networkLatency = Integer.parseInt(prop.getProperty("http.async.networkLatency"));
            profileThread = Integer.parseInt(prop.getProperty("http.async.profileThread"));
            reqThread = Integer.parseInt(prop.getProperty("http.async.reqThread"));
            singleRun = Boolean.parseBoolean(prop.getProperty("http.async.singleRun"));
            type = Integer.parseInt(prop.getProperty("http.type"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //scheduler.setThreadFactory(new BasicThreadFactory.Builder().namingPattern("Restmon Schedular - %d").build());
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(profileThread);
        //init metrics
        MetricStore.enableSupportabilityMetric();
        Sustanibility.initPublisher();

        if (singleRun) {
            new Schedule(getBatch(profileSize), executor).run();
        } else {
            scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.scheduleAtFixedRate(new Schedule(getBatch(profileSize), executor), 0, 2, TimeUnit.SECONDS);
        }
    }

    //number of profile input from restmon
    public static List<ProfileTask> getBatch(int size) {
        List<ProfileTask> profileTasks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            profileTasks.add(new ProfileTask("Profile-" + i, interval));
        }
        return profileTasks;
    }

    public static int getMaxUrls() {
        return maxUrls;
    }

    public static int getProfileSize() {
        return profileSize;
    }

    public static int getInterval() {
        return interval;
    }

    public static int getMaxTcpConnection() {
        return maxTcpConnection;
    }

    public static int getNetworkLatency() {
        return networkLatency;
    }

    public static int getProfileThread() {
        return profileThread;
    }

    public static int getReqThread() {
        return reqThread;
    }

    public static boolean isSingleRun() {
        return singleRun;
    }

    public static ScheduledThreadPoolExecutor getScheduler() {
        return scheduler;
    }

    public static ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public static long getPendingTaskCount() {
        return executor.getTaskCount() - executor.getCompletedTaskCount();
    }
}
