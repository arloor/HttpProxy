package com.arloor.forwardproxy.dnspod.param;

/**
 * https://docs.dnspod.cn/api/5f561f9ee75cf42d25bf6720/
 */
public class BaseParam {
    private String login_token = System.getenv("dnspod_token");
    private String format = "json";
    private String error_on_empty = "no";
    private String lang = "cn";

    public String getLogin_token() {
        return login_token;
    }

    public String getFormat() {
        return format;
    }

    public String getError_on_empty() {
        return error_on_empty;
    }

    public String getLang() {
        return lang;
    }
}
