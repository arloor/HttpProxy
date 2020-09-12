package com.arloor.forwardproxy;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ChannelHandler.Sharable
public class GlobalTrafficMonitor extends GlobalTrafficShapingHandler {
    private static GlobalTrafficMonitor instance = new GlobalTrafficMonitor(Executors.newScheduledThreadPool(1), 1000);

    public static GlobalTrafficMonitor getInstance() {
        return instance;
    }

    private static final int seconds = 600;
    private static List<String> xScales = new ArrayList<>();
    private static List<Double> yScales = new LinkedList<>();

    static {
        for (int i = 1; i <= seconds; i++) {
            xScales.add(String.valueOf(i));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(GlobalTrafficMonitor.class);

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
        long lastWriteThroughput = counter.lastWriteThroughput();
        yScales.add((double) lastWriteThroughput);
        if (yScales.size() > seconds) {
            yScales.remove(0);
        }
        super.doAccounting(counter);
    }

    public static final String html() {
        String legend = "上行网速";
        String legends = JSONObject.toJSONString(Lists.newArrayList(legend));
        String scales = JSONObject.toJSONString(xScales);
        String series = JSONObject.toJSONString(yScales);

        return RenderUtil.text(template, ImmutableMap.of("legends", legends, "scales", scales, "series", series));
    }

        private static final String template = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Netty-Server</title>\n" +
            "    <script src=\"https://cdn.staticfile.org/echarts/4.8.0/echarts.min.js\"></script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id=\"main\" style=\"width: 100%;height: 800px;\"></div>\n" +
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
            "                value[0].value = (value[0].value / Math.pow(k, c)).toPrecision(3) + ' ' + sizes[c];\n" +
            "            }\n" +
            "            //这里的value[0].name就是每次显示的name\n" +
            "            return value[0].name + \"<br/>\" + \"流量为:\" + value[0].value;\n" +
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
            "        max: function(value) {\n" +
            "            var k = 1024;\n" +
            "            var c = Math.floor(Math.log(value.max) / Math.log(k));\n" +
            "            interval = Math.pow(k, c);\n" +
            "            return Math.ceil(value.max / interval) * interval;\n" +
            "        },\n" +
            "        interval:1024*1024,\n" +
            "        axisLabel: {\n" +
            "            formatter: function(value, index) {\n" +
            "                if (value <= 0) {\n" +
            "                    value = '0B';\n" +
            "                } else {\n" +
            "                    var k = 1024;\n" +
            "                    var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                    //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                    var c = Math.floor(Math.log(value) / Math.log(k));\n" +
            "                    value = (value / Math.pow(k, c)).toPrecision(3) + ' ' + sizes[c];\n" +
            "                }\n" +
            "                //这里的value[0].name就是每次显示的name\n" +
            "                return value;\n" +
            "            }\n" +
            "        },\n" +
            "    },\n" +
            "    series: [{\n" +
            "        \"data\": [(${series})],\n" +
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
            "                        value = (value.value / Math.pow(k, c)).toPrecision(3) + ' ' + sizes[c];\n" +
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
            "            symbol:\"roundRect\",\n" +
            "            symbolSize:[70,30]," +
            "            \"label\": {\n" +
            "                formatter: function(value) {\n" +
            "                    if (value.value <= 0) {\n" +
            "                        value.value = '0B';\n" +
            "                    } else {\n" +
            "                        var k = 1024;\n" +
            "                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];\n" +
            "                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value\n" +
            "                        var c = Math.floor(Math.log(value.value) / Math.log(k));\n" +
            "                        value = (value.value / Math.pow(k, c)).toPrecision(3) + ' ' + sizes[c];\n" +
            "                    }\n" +
            "                    //这里的value[0].name就是每次显示的name\n" +
            "                    return value;\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"name\": \"上行网速\",\n" +
            "        \"smooth\": false,\n" +
            "        \"type\": \"line\"\n" +
            "    }],\n" +
            "    animationDuration: 10\n" +
            "};" +
            "    // 使用刚指定的配置项和数据显示图表。\n" +
            "    myChart.setOption(option);\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";
}
