package org.async;

import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.async.metric.MetricStore;
import org.asynchttpclient.*;
import org.asynchttpclient.filter.ThrottleRequestFilter;
import org.asynchttpclient.util.HttpConstants;
import sun.net.www.http.HttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.lang.Math.toIntExact;
import static org.async.metric.SupportabilityMetricConstant.API_TIME;
import static org.async.metric.SupportabilityMetricConstant.NUMBER_OF_API_CALL;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public class ExecuteUrlHttp implements ExecuteUrl {
    private ProfileTask task;
    private String url;
    private long id;
    private boolean isDone = false;

    private static final String AND = "&";
    private static final String EQUAL = "=";
    public static final String HTTP_QUERY_INDICATOR = "?";
    public static final String URL_SPACE_INDICATOR = "%20";

    ExecuteUrlHttp(ProfileTask task, String url, int id) {
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

    private final long restApiCallStart = System.currentTimeMillis();

    @Override
    public Object call() {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse clientresponse = null;
        long totalTime = System.currentTimeMillis();
        try {
            httpclient = getHttpClient(false, 5000);
            RequestConfig requestConfig = getRequestConfig(5000);
            URL urlEncode = encodeUrl(url);
            HttpRequestBase httpReq = buildHttpRequest(requestConfig,urlEncode);
            final HttpHost host = new HttpHost(urlEncode.getHost(), urlEncode.getPort(), urlEncode.getProtocol());
            clientresponse = httpclient.execute(host, httpReq, createClientContext(host,new BasicCredentialsProvider()));
            totalTime = totalTime - System.currentTimeMillis();
            if(HttpAsyn.getNetworkLatency()!=-1) {
                if (totalTime < (HttpAsyn.getNetworkLatency() * 1000)) {
                    Thread.sleep((HttpAsyn.getNetworkLatency() * 1000) - totalTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            closeConnection(clientresponse);
            closeConnection(httpclient);
            MetricStore.putMetric(NUMBER_OF_API_CALL, 1);
            MetricStore.putMetric(API_TIME, totalTime );
        }
        return clientresponse;
    }


    public HttpClientContext createClientContext(final HttpHost host, CredentialsProvider credsProvider) {
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(host, basicAuth);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        localContext.setCredentialsProvider(credsProvider);

        return localContext;
    }
    static URL encodeUrl(String u)
            throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {

        URL url;
        int idxQuery = u.indexOf(HTTP_QUERY_INDICATOR);
        if (idxQuery > 0) {
            url = new URL(
                    new URI(u.substring(0, idxQuery)).normalize().toString() + u.substring(idxQuery));
        } else {
            url = new URI(u).normalize().toURL();
        }

        // encode url correctly for Palo Alto url's that contain < and > (XML) in the url
        if (null!=url.getQuery()) {
            StringBuilder encodedQuery = new StringBuilder();
            for (String queryParam : url.getQuery().split(AND)) {
                if (encodedQuery.length() > 0) {
                    encodedQuery.append(AND);
                }
                String[] param = queryParam.split(EQUAL, 2);
                encodedQuery.append(param[0]);
                if (param.length > 1) {
                    encodedQuery.append(EQUAL);
                    encodedQuery.append(URLEncoder.encode(param[1], Charsets.UTF_8.toString()));
                }
            }

            String encodedUrl =
                    url.toExternalForm().split("\\" + HTTP_QUERY_INDICATOR)[0] + HTTP_QUERY_INDICATOR
                            + encodedQuery;
            url = new URL(encodedUrl.replaceAll("\\+", URL_SPACE_INDICATOR));
        }

        return url;
    }


    HttpRequestBase buildHttpRequest(RequestConfig requestConfig, URL url)
            throws Exception {


        HttpRequestBase httpReq;

        HttpGet httpGet = new HttpGet(url.toExternalForm());
        httpReq = httpGet;

        httpReq.setConfig(requestConfig);

        // set no-cache directive, want to tell intermediate proxies and server not to send cached response
        httpReq.addHeader(HttpHeaders.CACHE_CONTROL, "no-store");


        String uri = httpReq.getURI().toString();
        return httpReq;
    }

    public final CloseableHttpClient getHttpClient(boolean checkCerts, long retryTimeoutMs)
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return HttpClients.custom().setRetryHandler(retryHandler(retryTimeoutMs))
                .setDefaultCookieStore(new BasicCookieStore()).build();

    }
    private HttpRequestRetryHandler retryHandler(long timeoutMillis) {
        return (exception, executionCount, context) -> {
            if ((System.currentTimeMillis() - restApiCallStart) > timeoutMillis) {
                return false;
            } else if (exception instanceof NoHttpResponseException) {
                // Retry if the server dropped connection on us
                return true;
            }
            // otherwise do not retry
            return false;
        };
    }

    private RequestConfig getRequestConfig(long timeout) {
        // set circular redirects allowed to work in conjunction with the redirect handler created
        // this allows us to treat HTTP 202 responses as retries (ie. redirects back to itself)
        int timeoutMs = 10000;
        try {
            timeoutMs = toIntExact(timeout);
        } catch (ArithmeticException e) {
            e.printStackTrace();
        }
        return RequestConfig.custom()
                .setSocketTimeout(timeoutMs)
                .setConnectTimeout(timeoutMs)
                .setCircularRedirectsAllowed(true)
                .build();
    }

    private void closeConnection(Closeable cxn) {
        if (cxn != null) {
            try {
                cxn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

