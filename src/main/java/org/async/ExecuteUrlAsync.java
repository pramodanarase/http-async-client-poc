package org.async;

import org.asynchttpclient.*;
import org.asynchttpclient.filter.ThrottleRequestFilter;
import org.asynchttpclient.util.HttpConstants;

import java.net.URL;
import java.util.concurrent.*;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public class ExecuteUrlAsync implements ExecuteUrl {
    private ProfileTask task;
    private String url;
    private long id;
    private boolean isDone = false;
    public static AsyncHttpClient asynchttpclient = createHttpClient();

    ExecuteUrlAsync(ProfileTask task, String url, int id) {
        this.url = url;
        this.task = task;
        this.id = id;
    }

    public boolean isDone() {
        return isDone;
    }


    public void setDone(boolean done) {
        isDone = done;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Object call() {
        ListenableFuture<Response> futureResponse = null;
        try {
            futureResponse = testAsyncCall(task, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return futureResponse;
    }

    private ListenableFuture<Response> testAsyncCall(ProfileTask task, ExecuteUrlAsync url) throws Exception {
       /* if (asynchttpclient == null) {
            asynchttpclient = createHttpClient();
        }*/
        Request httpReq = buildHttpRequest(asynchttpclient, new URL(url.url));
        return asynchttpclient.executeRequest(httpReq, new ResponseHandler(task, url));
    }

    public static AsyncHttpClient createHttpClient() {

        asynchttpclient = asyncHttpClient(config()
                .setMaxConnections(HttpAsyn.getMaxTcpConnection())
                //persistent connection
                .setKeepAlive(true)
                //the maximum time in millisecond an AsyncHttpClient will keep connection idle in pool.
                .setPooledConnectionIdleTimeout(60 * 1000)
                // throttle request in queue if maxconnection
                .addRequestFilter(new ThrottleRequestFilter(HttpAsyn.getMaxTcpConnection()))
        );
        return asynchttpclient;
    }

    Request buildHttpRequest(AsyncHttpClient httpClient, URL url)
            throws Exception {
        RequestBuilder httpReq = null;
        httpReq = new RequestBuilder(HttpConstants.Methods.GET);
        httpReq.setUrl(url.toExternalForm());
        // set no-cache directive, want to tell intermediate proxies and server not to send cached response
        httpReq.addHeader("Cache-Control", "no-store");

        return httpReq.build();
    }
}

