package org.async;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.async.metric.MetricStore;
import org.async.metric.SupportabilityMetricConstant;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.netty.NettyResponseFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ProfileTask implements Callable<Void> {
    private volatile long startTime = 0;
    private volatile boolean isRunning = false;
    private int interval;
    private String profileName;
    //
    public static ThreadPoolExecutor requestExecutor;

    static {
        if (HttpAsyn.getReqThread() != 0) {
            requestExecutor = new ThreadPoolExecutor(HttpAsyn.getReqThread(), HttpAsyn.getReqThread(), 0L,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }
    }

    ProfileTask(String profileName, int interval) {
        this.interval = interval;
        this.profileName = profileName;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public String getProfileName() {
        return profileName;
    }


    public void run() {
        //divide urs per profile
        int hit = HttpAsyn.getMaxUrls() / HttpAsyn.getProfileSize();
        int b1 = (hit * 1) / 100;
        int b2 = (hit * 10) / 100;
        int b3 = (hit * 20) / 100;
        int b4 = hit - (b1 + b2 + b3);
        Thread.currentThread().setName("" + profileName);
        //empty urls
        String url = "https://www.random.org/integers/?num=" + 1 + "&min=0&max=65535&col=1&base=10&format=plain&rnd=new";
        url = "http://localhost:8080/";
        long time = System.currentTimeMillis();
        try {
            if (HttpAsyn.getReqThread() == 0) {
                executeWithoutRequestExecutors(b1, b2, b3, b4, url);
            } else {
                executeWithRequestExecutors(b1, b2, b3, b4, url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isRunning = false;
            long totalTime = System.currentTimeMillis() - time;
            //System.out.println("Finished -" + Thread.currentThread().getName() + " Time :" + totalTime + " Url : " + (1 + 10 + 89 + hit - 100));
            MetricStore.putMetric(SupportabilityMetricConstant.PROFILE_TIME, totalTime);
        }
    }

    public void executeWithRequestExecutors(int b1, int b2, int b3, int b4, String url) throws Exception {
        testIfResultReceived(requestExecutor.invokeAll(createUrlBatch(this, b1, url)));
        testIfResultReceived(requestExecutor.invokeAll(createUrlBatch(this, b2, url)));
        testIfResultReceived(requestExecutor.invokeAll(createUrlBatch(this, b3, url)));
        testIfResultReceived(requestExecutor.invokeAll(createUrlBatch(this, b4, url)));
    }

    public void executeWithoutRequestExecutors(int b1, int b2, int b3, int b4, String url) throws Exception {
        executeBatchUrls(createUrlBatch(this, b1, url));
        executeBatchUrls(createUrlBatch(this, b2, url));
        executeBatchUrls(createUrlBatch(this, b3, url));
        executeBatchUrls(createUrlBatch(this, b4, url));
    }

    private void executeBatchUrls(Collection<ExecuteUrl> urls) throws Exception {
        List<Object> list = new ArrayList<>();
        for (ExecuteUrl url : urls) {
            list.add(url.call());
        }
        if (HttpAsyn.type == 1) {
            testIfResultReceivedBatch(list);
        } else if (HttpAsyn.type == 2) {
            testIfResultReceivedBatch(list);
        }
    }

    void testIfResultReceivedBatch(List<Object> futures) throws Exception {
        for (Object f : futures) {
            try {
                if (f instanceof CloseableHttpResponse) {
                    CloseableHttpResponse response = (CloseableHttpResponse) f;
                    boolean isError = response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
                    if (isError)
                        System.out.println("Error...");
                } else if (f instanceof ListenableFuture) {
                    ListenableFuture<Response> futureResponse = (ListenableFuture<Response>) f;
                    // NettyResponseFuture<Response> responseListenableFuture = (NettyResponseFuture<Response>) f.get();
                    //wait to get result
                    Response response = futureResponse.get();

                } else {
                    Future future = (Future) f;
                    HttpResponse obj= (HttpResponse) future.get();
                    //System.out.println(obj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //System.out.println("Final:" + response.getResponseBody());
        }
    }

    void testIfResultReceived(List<Future<Object>> futures) throws Exception {
        for (Future f : futures) {
            try {
                Object future = f.get();
                if (HttpAsyn.type == 0) {
                    CloseableHttpResponse response = (CloseableHttpResponse) future;
                    boolean isError = response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
                    if (isError)
                        System.out.println("Error...");
                } else if(HttpAsyn.type ==2){
                    if(future instanceof  Future){
                        HttpResponse response =( (Future<HttpResponse>)future).get();
                    }
                    // Please note that it may be unsafe to access HttpContext instance
                    // while the request is still being executed
                }else{
                    NettyResponseFuture<Response> responseListenableFuture = (NettyResponseFuture<Response>) future;
                    //wait to get result
                    Response response = responseListenableFuture.get();

                    AsyncHandler handler = responseListenableFuture.getAsyncHandler();
                    if (handler instanceof ResponseHandler) {
                        ResponseHandler responseHandler = (ResponseHandler) handler;
                        if (!responseHandler.executeUrl.isDone()) {
                            System.out.println("Error" + responseHandler.task.getProfileName() + " url: " + responseHandler.executeUrl.getId());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //System.out.println("Final:" + response.getResponseBody());
        }
    }

    Collection<ExecuteUrl> createUrlBatch(ProfileTask task, int batch, String url) {
        Collection<ExecuteUrl> urlTask = new ArrayList<>();
        for (int i = 0; i < batch; i++) {
            urlTask.add(creteExeecuteUrlInstance(task, i, url));
        }
        return urlTask;
    }

    ExecuteUrl creteExeecuteUrlInstance(ProfileTask task, int i, String url) {
        ExecuteUrl executeUrl = null;
        if (HttpAsyn.type == 0) {
            executeUrl = new ExecuteUrlHttp(task, url, i);
        } else if (HttpAsyn.type == 1) {
            executeUrl = new ExecuteUrlAsync(task, url, i);
        } else {
            executeUrl = new ExecuteUrlApacheAsync(task, url, i);
        }
        return executeUrl;
    }

    @Override
    public Void call() throws Exception {
        run();
        return null;
    }

    public static long getRequestExecutorQueuSize() {
        long cnt = 0;
        if (requestExecutor != null) {
            cnt = requestExecutor.getTaskCount() - requestExecutor.getCompletedTaskCount();
        }
        return cnt;
    }
}

