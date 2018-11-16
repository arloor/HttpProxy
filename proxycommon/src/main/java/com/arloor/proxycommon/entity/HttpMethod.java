package com.arloor.proxycommon.entity;

public enum HttpMethod {
    CONNECT, GET, POST, PUT, HEAD, DELETE, OPTIONS, TRACE,NULLMETHOD;

    public static HttpMethod parseMethod(String method){
        if(method.equals("GET")){
            return GET;
        }
        if(method.equals("PUT")){
            return PUT;
        }
        if(method.equals("HEAD")){
            return HEAD;
        }
        if(method.equals("POST")){
            return POST;
        }
        if(method.equals("TRACE")){
            return TRACE;
        }
        if(method.equals("DELETE")){
            return DELETE;
        }
        if(method.equals("CONNECT")){
            return CONNECT;
        }
        if(method.equals("OPTIONS")){
            return OPTIONS;
        }
        return NULLMETHOD;
    }

    @Override
    public String toString() {
        switch (this) {
            case GET:
                return "GET";
            case PUT:
                return "PUT";
            case HEAD:
                return "HEAD";
            case POST:
                return "POST";
            case TRACE:
                return "TRACE";
            case DELETE:
                return "DELETE";
            case CONNECT:
                return "CONNECT";
            case OPTIONS:
                return "OPTIONS";
            default:
                return "NULLMETHOD";
        }
    }
}
