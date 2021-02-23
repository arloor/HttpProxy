package com.arloor.forwardproxy.monitor;

import com.google.common.collect.Lists;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * prometheus exporter实现类
 */
public class PromMonitorImpl implements MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(PromMonitorImpl.class);
    private static String hostname;
    static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

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
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        List<Metric<Long>> metrics = new ArrayList<>();
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
            metrics.add(new Metric<Long>(MetricType.gauge, "bufferPool使用量" + bufferPool.getName(), "bufferpool_used_" + fixName(bufferPool.getName()), bufferPool.getMemoryUsed()).tag("host", hostname));
            metrics.add(new Metric<Long>(MetricType.gauge, "bufferPool容量" + bufferPool.getName(), "bufferpool_capacity_" + fixName(bufferPool.getName()), bufferPool.getTotalCapacity()).tag("host", hostname));
        }
        metrics.addAll(procNetDevMetric());
        return metrics.stream().map(Metric::toString).collect(Collectors.joining());
    }

    private String fixName(String name) {
        String s = name.replaceAll(" ", "_").replaceAll("'", "").replaceAll("-", "_");
        return s;
    }

    private long nettyUsedDirectMemory() {
        return PlatformDependent.usedDirectMemory();
    }

    private static List<Metric<Long>> procNetDevMetric() {
        String filename = "/proc/net/dev";
        File file = new File(filename);
        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(file.toURI()));
                List<Metric<Long>> metrics = lines.stream()
                        .skip(2)
                        .map(line -> line.trim().replaceAll("(\\s)+", " ").split(" "))
                        .flatMap(splits -> {
                            if (splits.length == 17) {
                                String interfaceName = splits[0].substring(0, splits[0].length() - 1).replaceAll("-", "_");
                                return Lists.newArrayList(
                                        new Metric<Long>(MetricType.counter, "网卡流量", interfaceName + "_in_total", Long.parseLong(splits[1])).tag("host", hostname),
                                        new Metric<Long>(MetricType.counter, "网卡流量", interfaceName + "_out_total", Long.parseLong(splits[9])).tag("host", hostname)
                                ).stream();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                return metrics;
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        return new ArrayList<>();
    }
}
