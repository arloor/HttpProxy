package com.arloor.forwardproxy.monitor;

import com.alibaba.fastjson.JSONObject;
import com.arloor.forwardproxy.util.RenderUtil;
import com.arloor.forwardproxy.vo.RenderParam;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    private static String hostname;
    private static final int seconds = 500;
    private static List<String> xScales = new ArrayList<>();
    private static List<Double> yScalesUp = new LinkedList<>();
    private static List<Double> yScalesDown = new LinkedList<>();
    volatile long outTotal = 0L;
    volatile long inTotal = 0L;
    volatile long outRate = 0L;
    volatile long inRate = 0L;
    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
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

        RenderParam param = new RenderParam();
        param.add("legends", legends);
        param.add("scales", scales);
        param.add("seriesUp", seriesUp);
        param.add("seriesDown", seriesDown);
        param.add("interval", interval);
        param.add("title", hostname.length() > 10 ? hostname : hostname + " 实时网速");
        if (localEcharts) {
            param.add("echarts_url", "/echarts.min.js");
        } else {
            param.add("echarts_url", "https://cdn.staticfile.org/echarts/4.8.0/echarts.min.js");
        }
        return RenderUtil.text(TEMPLATE, param);
    }


    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>[(${title})]</title>
                <meta http-equiv="refresh" content="3">
                <script src="[(${echarts_url})]"></script>
            </head>
            <body style="margin: 0;height:100%;">
            <div id="main" style="width: 100%;height: 100vh;"></div>
            <script type="text/javascript">
                // 基于准备好的dom，初始化echarts实例
                var myChart = echarts.init(document.getElementById('main'));
                // 指定图表的配置项和数据
                var option = {
                    title: {
                        text: '[(${title})]'
                    },
                    tooltip: {
                        trigger: 'axis',
                        formatter: function(value) {
                            //这里的value[0].value就是我需要每次显示在图上的数据
                            if (value[0].value <= 0) {
                                value[0].value = '0B';
                            } else {
                                var k = 1024;
                                var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                //这里是取自然对数，也就是log（k）（value[0].value），求出以k为底的多少次方是value[0].value
                                var c = Math.floor(Math.log(value[0].value) / Math.log(k));
                                value[0].value = (value[0].value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];
                            }
                            if (value[1].value <= 0) {
                                value[1].value = '0B';
                            } else {
                                var k = 1024;
                                var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                //这里是取自然对数，也就是log（k）（value[0].value），求出以k为底的多少次方是value[0].value
                                var c = Math.floor(Math.log(value[1].value) / Math.log(k));
                                value[1].value = (value[1].value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];
                            }
                            //这里的value[0].name就是每次显示的name
                            return value[0].name + "<br/>" + "上行网速: " + value[0].value+ "<br/>" + "下行网速: " + value[1].value;
                        }
                    },
                    legend: {
                        data: [(${legends})]
                    },
                    toolbox: {
                        feature: {
                            mark: {
                                show: true
                            },
                            dataView: {
                                show: true,
                                readOnly: false
                            },
                            magicType: {
                                show: true,
                                type: ['line', 'bar']
                            },
                            restore: {
                                show: true
                            },
                            saveAsImage: {
                                show: true
                            }
                        }
                    },
                    xAxis: {
                        type: 'category',
                        boundaryGap: false,
                        data: [(${scales})]
                    },
                    yAxis: {
                        type: "value",
                        max: function(value) {
                            var k = 1024;
                            var c = Math.floor(Math.log(value.max) / Math.log(k));
                            interval = Math.pow(k, c);
                            return Math.ceil(value.max / interval) * interval;
                        },
                        interval: [(${interval})],
                        axisLabel: {
                            formatter: function(value, index) {
                                if (value <= 0) {
                                    value = '0B';
                                } else {
                                    var k = 1024;
                                    var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                    //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value
                                    var c = Math.floor(Math.log(value) / Math.log(k));
                                    value = (value / Math.pow(k, c)) + ' ' + sizes[c];
                                }
                                //这里的value[0].name就是每次显示的name
                                return value;
                            }
                        },
                    },
                    series: [        {
                        itemStyle:{
                            color: '#ef0000',
                        },
                        "data": [(${seriesUp})],
                        "markLine": {
                            "data": [{
                                "type": "average",
                                "name": "平均值"
                            }],
                            "label": {
                                formatter: function(value) {
                                    if (value.value <= 0) {
                                        value = '0B';
                                    } else {
                                        var k = 1024;
                                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value
                                        var c = Math.floor(Math.log(value.value) / Math.log(k));
                                        value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];
                                    }
                                    //这里的value[0].name就是每次显示的name
                                    return value;
                                }
                            }
                        },
                        "markPoint": {
                            "data": [{
                                "type": "max",
                                "name": "最大值"
                            }],
                            symbol: "roundRect",
                            symbolSize: [70, 30],
                            "label": {
                                formatter: function(value) {
                                    if (value.value <= 0) {
                                        value = '0B';
                                    } else {
                                        var k = 1024;
                                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value
                                        var c = Math.floor(Math.log(value.value) / Math.log(k));
                                        value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];
                                    }
                                    //这里的value[0].name就是每次显示的name
                                    return value;
                                }
                            }
                        },
                        "name": "上行网速",
                        "smooth": false,
                        "type": "line"
                    },
                    {
                        itemStyle:{
                            color: '#5bf',
                        },
                        "data": [(${seriesDown})],
                        "markLine": {
                            "data": [{
                                "type": "average",
                                "name": "平均值"
                            }],
                            "label": {
                                formatter: function(value) {
                                    if (value.value <= 0) {
                                        value = '0B';
                                    } else {
                                        var k = 1024;
                                        var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                        //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value
                                        var c = Math.floor(Math.log(value.value) / Math.log(k));
                                        value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];
                                    }
                                    //这里的value[0].name就是每次显示的name
                                    return value;
                                }
                            }
                        },
                        "markPoint": {
                             "data": [{
                                 "type": "max",
                                 "name": "最大值"
                             }],
                             symbol: "roundRect",
                             symbolSize: [70, 30],
                             "label": {
                                 formatter: function(value) {
                                     if (value.value <= 0) {
                                         value = '0B';
                                     } else {
                                         var k = 1024;
                                         var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
                                          //这里是取自然对数，也就是log（k）（value），求出以k为底的多少次方是value
                                         var c = Math.floor(Math.log(value.value) / Math.log(k));
                                         value = (value.value / Math.pow(k, c)).toPrecision(4) + ' ' + sizes[c];
                                     }
                                     //这里的value[0].name就是每次显示的name
                                     return value;
                                 }
                             }
                         },
                        "name": "下行网速",
                        "smooth": false,
                        "type": "line"
                    }],
                    animation: false,
                    animationDuration: 5
                };
                // 使用刚指定的配置项和数据显示图表。
                myChart.setOption(option);
            </script>
            </body>
            </html>
            """;
}
