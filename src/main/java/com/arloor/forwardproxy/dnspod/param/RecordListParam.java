package com.arloor.forwardproxy.dnspod.param;

public class RecordListParam extends BaseParam {
    private String domain = System.getenv("dnspod_domain");
    private String sub_domain = System.getenv("dnspod_subdomain");

    public String getDomain() {
        return domain;
    }

    public String getSub_domain() {
        return sub_domain;
    }
}
