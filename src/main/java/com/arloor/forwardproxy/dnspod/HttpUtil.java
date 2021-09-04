package com.arloor.forwardproxy.dnspod;

import com.arloor.forwardproxy.util.JsonUtil;
import com.google.common.collect.Lists;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;


public class HttpUtil {
    private static CloseableHttpClient client;
    private static final int READ_TIMEOUT = 100;
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    /**
     * 静态构造
     * 可以使用ssl
     */
    static {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // don't check
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // don't check
                    }
                }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAllCerts, null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(ctx);

        //连接管理器，设置总连接数和到单一host的最大连接数
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(50);

        //默认请求配置，这里设置cookie策略
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .setConnectionRequestTimeout(100)
                .setConnectTimeout(200) // 连接超时
                .setSocketTimeout(300) // 读超时
                .build();

        //创建httpclient
        client = HttpClients.custom()
                .setConnectionManager(cm)
                .setRetryHandler(buildRetryHandler(3))
                .setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(sslSocketFactory)
                .build();
    }

    private static HttpRequestRetryHandler buildRetryHandler(final int retryTime) {
        return (exception, executionCount, context) -> {
            if (executionCount >= retryTime) {
                return false;
            }
            if (exception instanceof NoHttpResponseException) {
                return true;
            }
            if (exception instanceof InterruptedIOException) {
                return true;
            }
            if (exception instanceof UnknownHostException) {
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                return false;
            }
            if (exception instanceof SSLException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            return !(request instanceof HttpEntityEnclosingRequest);
        };
    }

    /**
     * 发送post请求，请求体json,默认超时时间
     *
     * @param apiUrl
     * @param param
     * @return
     */
    public static String doPostJson(String apiUrl, Object param) throws IOException {
        return doPostJson(apiUrl, apiUrl, READ_TIMEOUT);
    }

    /**
     * 发送post请求，请求体json,超时时间
     *
     * @param apiUrl
     * @param param
     * @return
     */
    public static String doPostJson(String apiUrl, Object param, int readTimeout) throws IOException {
        String json = JsonUtil.toJson(param);
        String httpStr = null;
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;
        //设置超时时间
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(1000)
                .setConnectTimeout(1000)
                .setSocketTimeout(readTimeout)
                .build();
        httpPost.setConfig(config);

        try {
            StringEntity stringEntity = new StringEntity(json, "UTF-8");//解决中文乱码问题
            stringEntity.setContentType("application/json;charset=utf-8");
            httpPost.setEntity(stringEntity);
            response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            httpStr = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            log.error("HttpUtils post Failed!", e);
            throw e;
        } finally {
            if (response != null) {
                try {
                    // 确保entity里的inputStream close
                    EntityUtils.consume(response.getEntity());
                    //关闭response的底层connection【必要】
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //疑似没有必要，但框架HttpClientManager有这行
            httpPost.releaseConnection();
        }
        return Optional.ofNullable(httpStr).orElseGet(String::new);
    }

    public static String doPostForm(String apiUrl, Map<String, String> params, int readTimeout) throws IOException {
        String httpStr = null;
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;
        //设置超时时间
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(1000)
                .setConnectTimeout(1000)
                .setSocketTimeout(readTimeout)
                .build();
        httpPost.setConfig(config);

        try {

            List<BasicNameValuePair> data = Lists.newArrayList();
            params.forEach((key, value) -> {
                data.add(new BasicNameValuePair(key, value));
            });
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            httpStr = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            log.error("HttpUtils post Failed!", e);
            throw e;
        } finally {
            if (response != null) {
                try {
                    // 确保entity里的inputStream close
                    EntityUtils.consume(response.getEntity());
                    //关闭response的底层connection【必要】
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //疑似没有必要，但框架HttpClientManager有这行
            httpPost.releaseConnection();
        }
        return Optional.ofNullable(httpStr).orElseGet(String::new);
    }


    public static String get(String apiUrl, int readTimeout) throws IOException {
        String httpStr = null;
        HttpGet get = new HttpGet(apiUrl);
        CloseableHttpResponse response = null;
        //设置超时时间
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(1000)
                .setConnectTimeout(1000)
                .setSocketTimeout(readTimeout)
                .build();
        get.setConfig(config);

        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            httpStr = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            log.error("HttpUtils post Failed!", e);
            throw e;
        } finally {
            if (response != null) {
                try {
                    // 确保entity里的inputStream close
                    EntityUtils.consume(response.getEntity());
                    //关闭response的底层connection【必要】
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //疑似没有必要，但框架HttpClientManager有这行
            get.releaseConnection();
        }
        return Optional.ofNullable(httpStr).orElseGet(String::new);
    }


    public static void get(String apiUrl, int readTimeout, Consumer<InputStream> consumer) throws IOException {
        HttpGet get = new HttpGet(apiUrl);
        CloseableHttpResponse response = null;
        //设置超时时间
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(1000)
                .setConnectTimeout(1000)
                .setSocketTimeout(readTimeout)
                .build();
        get.setConfig(config);

        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            try (InputStream inputStream = entity.getContent();) {
                consumer.accept(inputStream);
            } catch (Throwable e) {
                log.error("cosume inputStream失败", e);
            }
        } catch (IOException e) {
            log.error("HttpUtils post Failed!", e);
            throw e;
        } finally {
            if (response != null) {
                try {
                    // 确保entity里的inputStream close
                    EntityUtils.consume(response.getEntity());
                    //关闭response的底层connection【必要】
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //疑似没有必要，但框架HttpClientManager有这行
            get.releaseConnection();
        }
    }
}