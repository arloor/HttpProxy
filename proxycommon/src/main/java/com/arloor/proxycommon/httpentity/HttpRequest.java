package com.arloor.proxycommon.httpentity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class HttpRequest {

    public static class HttpRequestHeader {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private String requestLine;

    private HttpMethod method;

    private String host;

    private int port;

    private String path;

    private List<HttpRequestHeader> headers;

    private byte[] requestBody;

    private void replaceKey(String oldKey, String newKey) {
        if (headers == null) {
            return;
        }
        for (HttpRequestHeader header : headers
        ) {
            if (header.getKey().equals(oldKey)) {
                header.setKey(newKey);
                break;
            }
        }
    }

    public void reform() {
        //处理因为代理而发生的http请求头变化
        replaceKey("Proxy-Connection", "Connection");
        if (requestLine != null) {
            String[] split = requestLine.split(" ");
            requestLine = split[0] + " " + path + " " + split[2];
        }
    }

    public String getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(String requestLine) {
        if (requestLine == null) {
            return;
        }
        this.requestLine = requestLine;
        this.method = HttpMethod.parseMethod(requestLine.substring(0, requestLine.indexOf(" ")));
        String urlStr = requestLine.substring(requestLine.indexOf(" ") + 1, requestLine.lastIndexOf(" "));
        this.path = urlStr;

        if (!method.equals(HttpMethod.CONNECT)) {
            if (!urlStr.startsWith("/")) {
                if (!urlStr.startsWith("http://")) {
                    urlStr = "http://" + urlStr;
                }
                try {
                    URL targetURL = new URL(urlStr);
                    this.host = targetURL.getHost();
                    this.port = targetURL.getPort() != -1 ? targetURL.getPort() : targetURL.getDefaultPort();
                    StringBuilder sb = new StringBuilder(targetURL.getPath());
                    if (targetURL.getQuery() != null) {
                        sb.append("?");
                        sb.append(targetURL.getQuery());
                    }
                    this.path = sb.toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (!urlStr.startsWith("https://")) {
                urlStr = "https://" + urlStr;
            }
            try {
                URL targetURL = new URL(urlStr);
                this.host = targetURL.getHost();
                this.port = targetURL.getPort() != -1 ? targetURL.getPort() : targetURL.getDefaultPort();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

    }

    public List<HttpRequestHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HttpRequestHeader> headers) {
        if (headers == null) {
            return;
        }
        this.headers = headers;
        for (HttpRequestHeader header : headers
        ) {
            if (header.getKey().equals("Host")) {
                String hostAndPort = header.getValue();
                String[] hostPortSplit = hostAndPort.split(":");
                //fix apt usr proxy
                //CONNECT download.docker.com:443 HTTP/1.1
                //Host: 127.0.0.1:8081
                //User-Agent: Debian APT-HTTP/1.3 (1.6.6)
                //fuck apt！
                if (this.host==null||"".equals(this.host)||!hostPortSplit[0].contains("127.0.0.1") && !hostPortSplit[0].startsWith("192.168.") && !hostPortSplit[0].startsWith("10.")) {
                    this.host = hostPortSplit[0];
                    if (this.method.equals(HttpMethod.CONNECT) && hostPortSplit.length == 1) {
                        this.port = 443;
                    } else {
                        this.port = Integer.parseInt(hostPortSplit.length == 2 ? hostPortSplit[1] : "80");//如果不带端口号则，默认为80
                    }
                }
                break;
            }
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public byte[] getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "body='" + (requestBody != null ? requestBody.length + "字节" : "null") + '\'' +
                ", method='" + method + '\'' +
                ", host='" + host + '\'' +
                ", serverport=" + port +
                ", path=" + path +
                "}";
    }


}
