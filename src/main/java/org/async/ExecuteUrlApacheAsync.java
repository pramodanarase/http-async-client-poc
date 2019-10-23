package org.async;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.protocol.HttpContext;

import java.net.URL;
import java.util.concurrent.Future;


public class ExecuteUrlApacheAsync implements ExecuteUrl {
    private ProfileTask task;
    private String url;
    private long id;
    private boolean isDone = false;
    public static CloseableHttpAsyncClient httpAsyncClient = createHttpClient();

    ExecuteUrlApacheAsync(ProfileTask task, String url, int id) {
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
        Future<HttpResponse> futureResponse = null;
        ResponseHandler responseHandler = new ResponseHandler(task,this);
        try {
            futureResponse = testAsyncCall(task, this, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return futureResponse;
    }

    private Future<HttpResponse> testAsyncCall(ProfileTask task, ExecuteUrlApacheAsync url, ResponseHandler responseHandler) throws Exception {
       /* if (httpAsyncClient == null) {
            httpAsyncClient = createHttpClient();
        }*/
        HttpRequestBase httpReq = buildHttpRequest(httpAsyncClient, new URL(url.url));
        HttpContext context = HttpClientContext.create();
        return httpAsyncClient.execute(httpReq, context,responseHandler);
    }

    public static CloseableHttpAsyncClient createHttpClient(){
      //https://github.com/AsyncHttpClient/async-http-client/blob/master/client/src/main/resources/org/asynchttpclient/config/ahc-default.properties

        // Create I/O reactor configuration
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2)
                .setConnectTimeout(30000)
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)
                .setSoReuseAddress(false)
                .setSoTimeout(30000)
                .build();
        try {
            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);

            PoolingNHttpClientConnectionManager connManager =
                    new PoolingNHttpClientConnectionManager(ioReactor);


            // Configure total max or per route limits for persistent connections
            // that can be kept in the pool or leased by the connection manager.
            connManager.setMaxTotal(HttpAsyn.getMaxTcpConnection());

            httpAsyncClient =
                    HttpAsyncClients.custom().setConnectionManager(connManager).build();
            httpAsyncClient.start();

        }catch (Exception e){
            e.printStackTrace();
        }

        return httpAsyncClient;
    }

    HttpRequestBase buildHttpRequest(CloseableHttpAsyncClient httpClient, URL url)
            throws Exception {
        HttpRequestBase httpReq;
        HttpGet httpGet = new HttpGet(url.toExternalForm());
            httpReq = httpGet;
               // set no-cache directive, want to tell intermediate proxies and server not to send cached response
        httpReq.addHeader("Cache-Control", "no-store");
       // httpReq.setConfig(requestConfig);
        httpReq.addHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return httpReq;
    }
}

