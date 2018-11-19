package com.arloor.proxycommon.httpentity;

import java.util.Date;

public class HttpResponse {

    public static byte[] ESTABLISHED() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 Connection Established\r\n");
        sb.append("Proxy-agent: https://github.com/arloor/proxynew\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    public static byte[] ERROR404() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 404\r\n" +
                "Content-Type: text/html;charset=ISO-8859-1\r\n" +
                "Content-Language: zh\r\n" +
                "Content-Length: 254\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                "<html><body><h1>Whitelabel Error Page</h1><p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p><div>There was an unexpected error (type=Not Found, status=404).</div><div>No message available</div></body></html>");
        return sb.toString().getBytes();
    }

    public static byte[] ERROR503() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.0 503 Error\r\n");
        sb.append("Content-Language: zh-CN\r\n");
        sb.append("Content-Length: 656\r\n");
        sb.append("text/html;charset=ISO-8859-1\r\n");
        sb.append("\r\n");
        sb.append("<html>\n" +
                "<head>\n" +
                "    <title>Proxnew Error Report</title>\n" +
                "    <style type=\"text/css\">\n" +
                "body {\n" +
                "    font-family: Arial,Helvetica,Sans-serif;\n" +
                "    font-size: 12px;\n" +
                "    color: #333333;\n" +
                "    background-color: #ffffff;\n" +
                "}\n" +
                "\n" +
                "h1 {\n" +
                "    font-size: 24px;\n" +
                "    font-weight: bold;\n" +
                "}\n" +
                "\n" +
                "h2 {\n" +
                "    font-size: 18px;\n" +
                "    font-weight: bold;\n" +
                "}\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Proxynew Error Report</h1>\n" +
                "<h2>Proxynew cannot process the request</h2>\n" +
                "<p>Proxynew failed to resolve the name of the remote host into an IP address. Check that the URL is correct.</p>\n" +
                "\n" +
                "<p>\n" +
                "<i>Proxynew, <a href=\"https://github.com/arloor/proxynew\">https://github.com/arloor/proxynew</a></i>\n" +
                "</p>\n" +
                "</body>\n" +
                "</html>");
        return sb.toString().getBytes();
    }
}
