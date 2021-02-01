package com.arloor.forwardproxy.monitor;

import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromMonitorImpl implements MonitorService {
    private static String hostname;
    static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
    private static final Logger logger = LoggerFactory.getLogger(PromMonitorImpl.class);

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static final class Metric<T extends Number> {
        private MetricType type;
        private String help;
        private String name;
        private Map<String, String> tags = new HashMap<>();
        private T value;


        public Metric(MetricType type, String help, String name, T value) {
            this.type = type;
            this.help = help;
            this.name = name;
            this.value = value;
        }

        public Metric<T> tag(String key, String value) {
            tags.put(key, value);
            return this;
        }

        @Override
        public String toString() {
            return "# HELP " + name + " " + help + "\n" +
                    "# TYPE " + name + " " + type + "\n" +
                    nameTags() + " " + getValue() + "\n";
        }

        private String getValue() {
            if (value == null) {
                return "0";
            } else {
                return String.valueOf(value);
            }
        }

        private String nameTags() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append("{");
            for (Map.Entry<String, String> tagvalue : tags.entrySet()) {
                sb.append(tagvalue.getKey());
                sb.append("=\"");
                sb.append(tagvalue.getValue());
                sb.append("\",");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    private enum MetricType {
        gauge, counter
    }

    @Override
    public String metrics() {
        for (BufferPoolMXBean bufferPoolMXBean : bufferPoolMXBeans) {
            logger.info("buffer pool: {} {} {}", bufferPoolMXBean.getName(), bufferPoolMXBean.getMemoryUsed(), bufferPoolMXBean.getTotalCapacity());
        }
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        List<Metric> metrics = new ArrayList<>();
        metrics.add(new Metric<>(MetricType.counter, "上行流量", "proxy_out", GlobalTrafficMonitor.getInstance().outTotal).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.counter, "下行流量", "proxy_in", GlobalTrafficMonitor.getInstance().inTotal).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "上行网速", "proxy_out_rate", GlobalTrafficMonitor.getInstance().outRate).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "下行网速", "proxy_in_rate", GlobalTrafficMonitor.getInstance().inRate).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "netty直接内存 对于jdk9+，请增加-Dio.netty.tryReflectionSetAccessible=true", "direct_memory_total", nettyUsedDirectMemory()).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "堆内存使用量", "heap_memory_usage", heapMemoryUsage.getUsed()).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "堆内存容量", "heap_memory_committed", heapMemoryUsage.getCommitted()).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "非堆内存使用量", "nonheap_memory_usage", nonHeapMemoryUsage.getUsed()).tag("host", hostname));
        metrics.add(new Metric<>(MetricType.gauge, "非堆内存容量", "nonheap_memory_committed", nonHeapMemoryUsage.getCommitted()).tag("host", hostname));
        for (BufferPoolMXBean bufferPool : bufferPoolMXBeans) {
            logger.info("buffer pool: {} {} {}", bufferPool.getName(), bufferPool.getMemoryUsed(), bufferPool.getTotalCapacity());
            metrics.add(new Metric(MetricType.gauge, "bufferPool使用量" + bufferPool.getName(), "bufferpool_used_" + fixName(bufferPool.getName()), bufferPool.getMemoryUsed()).tag("host", hostname));
            metrics.add(new Metric(MetricType.gauge, "bufferPool容量" + bufferPool.getName(), "bufferpool_capacity_" + fixName(bufferPool.getName()), bufferPool.getTotalCapacity()).tag("host", hostname));
        }
        return metrics.stream().map(Metric::toString).collect(Collectors.joining());
    }

    private String fixName(String name){
        String s = name.replaceAll(" ", "_").replaceAll("'", "").replaceAll("-","_");
        return s;
    }

    private long nettyUsedDirectMemory() {
        return PlatformDependent.usedDirectMemory();
    }

}
