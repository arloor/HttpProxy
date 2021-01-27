package com.arloor.forwardproxy.monitor;

import java.util.ServiceLoader;

public interface MonitorService {

    void hello();

    static MonitorService getInstance() {
        ServiceLoader<MonitorService> MonitorServices = ServiceLoader.load(MonitorService.class);
        if (MonitorServices.iterator().hasNext()) {
            return MonitorServices.iterator().next();
        }
        return null;
    }
}
