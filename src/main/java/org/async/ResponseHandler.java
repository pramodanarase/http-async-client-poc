package org.async;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.async.metric.MetricStore;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Response;

import java.util.concurrent.atomic.AtomicLong;

import static org.async.metric.SupportabilityMetricConstant.API_TIME;
import static org.async.metric.SupportabilityMetricConstant.NUMBER_OF_API_CALL;

public class ResponseHandler extends AsyncCompletionHandler implements FutureCallback<HttpResponse> {
    private static AtomicLong errorCount = new AtomicLong(0);
    ProfileTask task;
    ExecuteUrl executeUrl;
    long time;

    ResponseHandler(ProfileTask task, ExecuteUrl executeUrl) {
        this.task = task;
        this.executeUrl = executeUrl;
        time = System.currentTimeMillis();
        MetricStore.putMetric(NUMBER_OF_API_CALL, 1);
    }

    @Override
    public Object onCompleted(Response response) throws Exception {
        processResponse();
        return response;
    }

    private void processResponse() throws Exception {
        long totalTime = System.currentTimeMillis() - time;
        if (HttpAsyn.getNetworkLatency() != -1) {
            if (totalTime < (HttpAsyn.getNetworkLatency() * 1000)) {
                Thread.sleep((HttpAsyn.getNetworkLatency() * 1000) - totalTime);
            }
        }
        //System.out.println(Thread.currentThread().getName()+  " :"+ task.getProfileName() + " Urlid: "+ executeUrl.getId()+ " Req time: " + (System.currentTimeMillis() - time));
        executeUrl.setDone(true);
        MetricStore.putMetric(API_TIME, totalTime);
    }

    @Override
    public void onThrowable(Throwable t) {
        System.err.println("Error count: " + errorCount.incrementAndGet() + "" + t.getMessage());
    }

    @Override
    public void completed(HttpResponse result) {
        try {
            processResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void failed(Exception ex) {
        System.err.println("Error count: " + errorCount.incrementAndGet() + "" + ex.getMessage());
    }

    @Override
    public void cancelled() {
        System.err.println("Cancelled count: " + errorCount.incrementAndGet());
    }
}