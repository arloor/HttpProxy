package com.arloor.forwardproxy.dnspod;


import com.arloor.forwardproxy.dnspod.param.RecordListParam;
import com.arloor.forwardproxy.dnspod.result.RecordList;
import com.arloor.forwardproxy.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class DnspodHelper {
    private static final Logger log = LoggerFactory.getLogger(DnspodHelper.class);
    private static final String API_URL = "https://dnsapi.cn/";


    public static RecordList fetchRecordList() throws IOException {
        String content = HttpUtil.doPostForm(API_URL + "Record.List", JsonUtil.fromJson(JsonUtil.toJson(new RecordListParam()), HashMap.class), 5000);
        return JsonUtil.fromJson(content, RecordList.class);
    }

    public static String fetchCurrentIp() throws IOException {
        String content = HttpUtil.get("https://sg.gcall.me", 5000);
        return content;
    }

    public static void ddns() {
        try {
            RecordList recordList = fetchRecordList();
            for (RecordList.Record record : recordList.getRecords()) {
                if (record.getName().equals(getSubdomain())) {
                    String lastIp = record.getValue();
                    String currentIp = fetchCurrentIp();
                    if (!Objects.equals(currentIp, lastIp)) {
                        modifyRecord(record, currentIp);
                    } else {
                        log.info("ip未变化：{} @{}", lastIp, new Date());
                    }
                }
            }
        } catch (Throwable e) {
            log.error("update dns error!", e);
        }
    }

    private static void modifyRecord(RecordList.Record record, String currentIp) throws IOException {
        HashMap<String, String> ddnsParam = new HashMap<>();
        ddnsParam.put("login_token", getToken());
        ddnsParam.put("format", "json");
        ddnsParam.put("error_on_empty", "no");
        ddnsParam.put("domain", getDomain());
        ddnsParam.put("record_id", record.getId());
        ddnsParam.put("sub_domain", getSubdomain());
        ddnsParam.put("record_line_id", record.getLine_id());
        ddnsParam.put("value", currentIp);
        String s = HttpUtil.doPostForm(API_URL + "Record.Ddns", ddnsParam, 5000);
        log.info(s);
    }

    private static String getSubdomain() {
        return System.getenv("dnspod_subdomain");
    }

    public static String getDomain() {
        return System.getenv("dnspod_domain");
    }

    public static String getToken() {
        return System.getenv("dnspod_token");
    }

    public static boolean isEnable() {
        return getSubdomain() != null && getDomain() != null && getToken() != null;
    }

}
