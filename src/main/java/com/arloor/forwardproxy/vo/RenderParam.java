package com.arloor.forwardproxy.vo;

import java.util.HashMap;
import java.util.Map;

public final class RenderParam {
    private Map<String, Object> map = new HashMap<>();

    public Map<String, Object> getContent() {
        return map;
    }

    public RenderParam add(String key, Object value) {
        map.put(key, value);
        return this;
    }
}
