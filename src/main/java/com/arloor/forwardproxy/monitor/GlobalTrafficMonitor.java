package com.arloor.forwardproxy.monitor;

import com.alibaba.fastjson.JSONObject;
import com.arloor.forwardproxy.util.RenderUtil;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

@ChannelHandler.Sharable
/**
 * 该应用的网速监控
 */
public class GlobalTrafficMonitor extends GlobalTrafficShapingHandler {
    private static GlobalTrafficMonitor instance = new GlobalTrafficMonitor(MonitorService.EXECUTOR_SERVICE, 1000);

    public static GlobalTrafficMonitor getInstance() {
        return instance;
    }

    private static final int seconds = 500;
    private static List<String> xScales = new ArrayList<>();
    private static List<Double> yScalesUp = new LinkedList<>();
    private static List<Double> yScalesDown = new LinkedList<>();
    volatile long outTotal = 0L;
    volatile long inTotal = 0L;
    volatile long outRate = 0L;
    volatile long inRate = 0L;

    static {
        for (int i = 1; i <= seconds; i++) {
            xScales.add(String.valueOf(i));
        }
    }


    private GlobalTrafficMonitor(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval, long maxTime) {
        super(executor, writeLimit, readLimit, checkInterval, maxTime);
    }

    private GlobalTrafficMonitor(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval) {
        super(executor, writeLimit, readLimit, checkInterval);
    }

    private GlobalTrafficMonitor(ScheduledExecutorService executor, long writeLimit, long readLimit) {
        super(executor, writeLimit, readLimit);
    }

    private GlobalTrafficMonitor(ScheduledExecutorService executor, long checkInterval) {
        super(executor, checkInterval);
    }

    private GlobalTrafficMonitor(EventExecutor executor) {
        super(executor);
    }

    @Override
    protected void doAccounting(TrafficCounter counter) {
        synchronized (this) {
            long lastWriteThroughput = counter.lastWriteThroughput();
            outRate = lastWriteThroughput;
            yScalesUp.add((double) lastWriteThroughput);
            if (yScalesUp.size() > seconds) {
                yScalesUp.remove(0);
            }
            long lastReadThroughput = counter.lastReadThroughput();
            inRate = lastReadThroughput;
            yScalesDown.add((double) lastReadThroughput);
            if (yScalesDown.size() > seconds) {
                yScalesDown.remove(0);
            }
            outTotal = counter.cumulativeWrittenBytes();
            inTotal = counter.cumulativeReadBytes();
        }
        super.doAccounting(counter);
    }

    private static long getDirectMemoryCounter() {
        return PlatformDependent.usedDirectMemory();
    }

    public static final String html(boolean localEcharts) {
        String legends = JSONObject.toJSONString(Lists.newArrayList("上行网速", "下行网速"));
        String scales = JSONObject.toJSONString(xScales);
        String seriesUp = JSONObject.toJSONString(yScalesUp);
        String seriesDown = JSONObject.toJSONString(yScalesDown);

        long interval = 1024 * 1024;
        Double upMax = yScalesUp.stream().max(Double::compareTo).orElse(0D);
        Double downMax = yScalesDown.stream().max(Double::compareTo).orElse(0D);
        Double max = Math.max(upMax, downMax);
        if (max / (interval) > 10) {
            interval = (long) Math.ceil(max / interval / 10) * interval;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("legends", legends);
        params.put("scales", scales);
        params.put("seriesUp", seriesUp);
        params.put("seriesDown", seriesDown);
        params.put("interval", interval);
        if (localEcharts) {
            params.put("echarts_url", "/echarts.min.js");
        } else {
            params.put("echarts_url", "https://cdn.staticfile.org/echarts/4.8.0/echarts.min.js");
        }
        return RenderUtil.text(template, params);
    }

    private static final String template = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<title>实时网速</title>\n" +
            "<meta http-equiv=\"refresh\" content=\"3\">" +
            "<script src=\"[(${echarts_url})]\"></script>\n" +
            "</head>\n" +
            "<body style=\"margin: 0;height:100%;\">\n" +
            "<div id=\"main\" style=\"width: 100%;height: 100vh;\"></div>\n" +
            "<script type=\"text/javascript\">\n" +
            "    // 基于准备好的dom，初始化echarts实例\n" +
            "    var myChart = echarts.init(document.getElementById('main'));\n" +
            "    // 指定图表的配置项和数据\n" +
            "var option = {\n" +
            "    title: {\n" +
            "        text: '网速监控'\n" +
            "    },\n" +
            "    tooltip: {\n" +
            "        trigger: 'axis',\n" +
            "        formatter: function(value) {\n" +
            "            //这里的value[0].value就是我需要每次显示在图上的数据\n" +
            "            if (value[0].value <= 0) {\n" +
            "                value[0].value = '0B';\n" +
            "            } else {\n" +
            "                var k = 1024;\n" +
            "                var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                //这里是取自然对数，也就是log（k）（value[0].value），求出以k为底的多少次方是value[0].value\n" +
            "                var c = Math.floor(Math.log(value[0].value) / Math.log(k));\n" +
            "                value[0].value = (value[0].value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];\n" +
            "            }\n" +
            "            if (value[1].value <= 0) {\n" +
            "                value[1].value = '0B';\n" +
            "            } else {\n" +
            "                var k = 1024;\n" +
            "                var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                //这里是取自然对数，也就是log（k）（value[0].value），求出以k为底的多少次方是value[0].value\n" +
            "                var c = Math.floor(Math.log(value[1].value) / Math.log(k));\n" +
            "                value[1].value = (value[1].value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];\n" +
            "            }\n" +
            "            //这里的value[0].name就是每次显示的name\n" +
            "            return value[0].name + \"<br/>\" + \"上行网速: \" + value[0].value+ \"<br/>\" + \"下行网速: \" + value[1].value;\n" +
            "        }\n" +
            "    },\n" +
            "    legend: {\n" +
            "        data: [(${legends})]\n" +
            "    },\n" +
            "    toolbox: {\n" +
            "        feature: {\n" +
            "            mark: {\n" +
            "                show: true\n" +
            "            },\n" +
            "            dataView: {\n" +
            "                show: true,\n" +
            "                readOnly: false\n" +
            "            },\n" +
            "            magicType: {\n" +
            "                show: true,\n" +
            "                type: ['line', 'bar']\n" +
            "            },\n" +
            "            restore: {\n" +
            "                show: true\n" +
            "            },\n" +
            "            saveAsImage: {\n" +
            "                show: true\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    xAxis: {\n" +
            "        type: 'category',\n" +
            "        boundaryGap: false,\n" +
            "        data: [(${scales})]\n" +
            "    },\n" +
            "    yAxis: {\n" +
            "        type: \"value\",\n" +
            // start: 以1MB为分割
            "        max: function(value) {\n" +
            "            var k = 1024;\n" +
            "            var c = Math.floor(Math.log(value.max) / Math.log(k));\n" +
            "            interval = Math.pow(k, c);\n" +
            "            return Math.ceil(value.max / interval) * interval;\n" +
            "        },\n" +
            "        interval: [(${interval})],\n" + // 1MB
            "        axisLabel: {\n" +
            "            formatter: function(value, index) {\n" +
            "                if (value <= 0) {\n" +
            "                    value = '0B';\n" +
            "                } else {\n" +
            "                    var k = 1024;\n" +
            "                    var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                    //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                    var c = Math.floor(Math.log(value) / Math.log(k));\n" +
            "                    value = (value / Math.pow(k, c)) + ' ' + sizes[c];\n" +
            "                }\n" +
            "                //这里的value[0].name就是每次显示的name\n" +
            "                return value;\n" +
            "            }\n" +
            "        },\n" +
            // end： 以1MB为分割
            "    },\n" +
            "    series: [" +
            "        {\n" +
            "        itemStyle:{\n" +
            "            color: '#ef0000',\n" + //上行流量 红色
            "        },\n" +
            "        \"data\": [(${seriesUp})],\n" +
            "        \"markLine\": {\n" +
            "            \"data\": [{\n" +
            "                \"type\": \"average\",\n" +
            "                \"name\": \"平均值\"\n" +
            "            }],\n" +
            "            \"label\": {\n" +
            "                formatter: function(value) {\n" +
            "                    if (value.value <= 0) {\n" +
            "                        value.value = '0B';\n" +
            "                    } else {\n" +
            "                        var k = 1024;\n" +
            "                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                        var c = Math.floor(Math.log(value.value) / Math.log(k));\n" +
            "                        value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];\n" +
            "                    }\n" +
            "                    //这里的value[0].name就是每次显示的name\n" +
            "                    return value;\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"markPoint\": {\n" +
            "            \"data\": [{\n" +
            "                \"type\": \"max\",\n" +
            "                \"name\": \"最大值\"\n" +
            "            }],\n" +
            "            symbol: \"roundRect\",\n" +
            "            symbolSize: [70, 30],\n" +
            "            \"label\": {\n" +
            "                formatter: function(value) {\n" +
            "                    if (value.value <= 0) {\n" +
            "                        value.value = '0B';\n" +
            "                    } else {\n" +
            "                        var k = 1024;\n" +
            "                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                        var c = Math.floor(Math.log(value.value) / Math.log(k));\n" +
            "                        value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];\n" +
            "                    }\n" +
            "                    //这里的value[0].name就是每次显示的name\n" +
            "                    return value;\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"name\": \"上行网速\",\n" +
            "        \"smooth\": false,\n" +
            "        \"type\": \"line\"\n" +
            "    },\n" +
            "    {\n" +
            "        itemStyle:{\n" +
            "            color: '#5bf',\n" + //下行流量 蓝色
            "        },\n" +
            "        \"data\": [(${seriesDown})],\n" +
            "        \"markLine\": {\n" +
            "            \"data\": [{\n" +
            "                \"type\": \"average\",\n" +
            "                \"name\": \"平均值\"\n" +
            "            }],\n" +
            "            \"label\": {\n" +
            "                formatter: function(value) {\n" +
            "                    if (value.value <= 0) {\n" +
            "                        value.value = '0B';\n" +
            "                    } else {\n" +
            "                        var k = 1024;\n" +
            "                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                        var c = Math.floor(Math.log(value.value) / Math.log(k));\n" +
            "                        value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];\n" +
            "                    }\n" +
            "                    //这里的value[0].name就是每次显示的name\n" +
            "                    return value;\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"markPoint\": {\n" +
            "             \"data\": [{\n" +
            "                 \"type\": \"max\",\n" +
            "                 \"name\": \"最大值\"\n" +
            "             }],\n" +
            "             symbol: \"roundRect\",\n" +
            "             symbolSize: [70, 30],\n" +
            "             \"label\": {\n" +
            "                 formatter: function(value) {\n" +
            "                     if (value.value <= 0) {\n" +
            "                         value.value = '0B';\n" +
            "                     } else {\n" +
            "                         var k = 1024;\n" +
            "                         var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                          //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                         var c = Math.floor(Math.log(value.value) / Math.log(k));\n" +
            "                         value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];\n" +
            "                     }\n" +
            "                     //这里的value[0].name就是每次显示的name\n" +
            "                     return value;\n" +
            "                 }\n" +
            "             }\n" +
            "         },\n" +
            "        \"name\": \"下行网速\",\n" +
            "        \"smooth\": false,\n" +
            "        \"type\": \"line\"\n" +
            "    }],\n" +
            "    animation: false,\n" +
            "    animationDuration: 5\n" +
            "};\n" +
            "    // 使用刚指定的配置项和数据显示图表。\n" +
            "    myChart.setOption(option);\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";
}
