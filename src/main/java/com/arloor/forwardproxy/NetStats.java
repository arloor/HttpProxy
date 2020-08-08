package com.arloor.forwardproxy;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NetStats {
    private static final String filename="/proc/net/dev";
    private static List<String> interfaces =new ArrayList<>();
    private static List<String> xScales =new ArrayList<>();
    private static final int seconds =120;
    static {
        for (int i = 1; i <=seconds ; i++) {
            xScales.add(String.valueOf(i));
        }
    }
    private static class YValue {
        String name;
        List<Double> data;
        String type="line";
        boolean smooth=false;

        public YValue(String name, List<Double> data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public List<Double> getData() {
            return data;
        }

        public String getType() {
            return type;
        }

        public boolean isSmooth() {
            return smooth;
        }
    }

    private static final Map<String, List<Double>> inSpeedMap =new HashMap<>();
    private static final Map<String, List<Double>> outSpeedMap =new HashMap<>();
    private static final Map<String, Long> interIn=new HashMap<>();
    private static final Map<String, Long> interOut=new HashMap<>();

    public static Runnable task = () -> {
        File file =new File(filename);
        if(file.exists()){
            try {
                List<String> lines = Files.readAllLines(Paths.get(file.toURI()));
                interfaces= lines.stream().skip(2).map(line->line.replaceAll("(\\s)+", ",").split(",")[1])
                        .filter(eth->!eth.startsWith("lo:"))
                        .flatMap(eth->{
                    ArrayList<String> objects = new ArrayList<>();
                    objects.add(eth+"入");
                    objects.add(eth+"出");
                    return objects.stream();
                }).collect(Collectors.toList());
//                interfaces.stream().forEach(System.out::println);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true){
                try {
                    List<String> lines = Files.readAllLines(Paths.get(file.toURI()));
                    lines.stream().skip(2).forEach((line)->{
                        line = line.replaceAll("(\\s)+", ",");
                        String[] split=line.split(",");
                        String eth = split[1];
                        if(eth.startsWith("lo:")){
                            return;
                        }
                        long in = Long.parseLong(split[2]);
                        long out = Long.parseLong(split[10]);
                        long oldOut = interOut.getOrDefault(eth, (long) 0);
                        long oldIn = interIn.getOrDefault(eth, (long) 0);
                        long outChange = out - oldOut;
                        long inChange = in -oldIn;
                        if(oldIn!=0){
                            inSpeedMap.computeIfAbsent(eth, s -> new ArrayList<Double>());
                            inSpeedMap.get(eth).add((double)(inChange/1024));
                            if(inSpeedMap.get(eth).size()>seconds){
                                inSpeedMap.get(eth).remove(0);
                            }
                        }
                        if(oldOut!=0){
                            outSpeedMap.computeIfAbsent(eth, s -> new ArrayList<Double>());
                            outSpeedMap.get(eth).add((double)(outChange/1024));
                            if(outSpeedMap.get(eth).size()>seconds){
                                outSpeedMap.get(eth).remove(0);
                            }
                        }
                        interIn.put(eth,in);
                        interOut.put(eth,out);
                    });
                    Thread.sleep(1000);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    };

    private static final List<YValue> buildYvalues(){
        List<YValue> YValues=new ArrayList<>();
        inSpeedMap.entrySet().forEach(entry->{
            String eth =entry.getKey();
            List<Double> speeds=entry.getValue();
            YValue yValue =new YValue(eth+"入",speeds);
            YValues.add(yValue);
        });
        outSpeedMap.entrySet().forEach(entry->{
            String eth =entry.getKey();
            List<Double> speeds=entry.getValue();
            YValue yValue =new YValue(eth+"出",speeds);
            YValues.add(yValue);
        });
        return YValues;
    }

    public static final void start(){
        new Thread(NetStats.task).start();
    }

    public static final String html(){
        List<YValue> yValues = buildYvalues();
        String legends = JSONObject.toJSONString(interfaces);
        String scales = JSONObject.toJSONString(xScales);
        String series=JSONObject.toJSONString(yValues);

        String template="<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>server_status</title>\n" +
                "    <script src=\"https://cdn.staticfile.org/echarts/4.8.0/echarts.min.js\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"main\" style=\"width: 100%;height: 800px;\"></div>\n" +
                "<script type=\"text/javascript\">\n" +
                "    // 基于准备好的dom，初始化echarts实例\n" +
                "    var myChart = echarts.init(document.getElementById('main'));\n" +
                "    // 指定图表的配置项和数据\n" +
                "    var option = {\n" +
                "        title: {\n" +
                "            text: '网卡网速（KB/s）'\n" +
                "        },\n" +
                "        tooltip: {\n" +
                "            trigger: 'axis'\n" +
                "        },\n" +
                "        legend: {\n" +
                "            data: "+legends+"\n" +
                "        },\n" +
                "        toolbox: {\n" +
                "            feature: {\n" +
                "                mark : {show: true},\n" +
                "                dataView : {show: true, readOnly: false},\n" +
                "                magicType : {show: true, type: ['line', 'bar']},\n" +
                "                restore : {show: true},\n" +
                "                saveAsImage : {show: true}" +
                "            }\n" +
                "        },\n" +
                "        xAxis: {\n" +
                "            type: 'category',\n" +
                "            boundaryGap: false,\n" +
                "            data: "+scales+"\n" +
                "        },\n" +
                "        yAxis: {\n" +
                "            type: \"value\"\n" +
                "        },\n" +
                "        series: "+series+"\n" +
                "    };\n" +
                "    // 使用刚指定的配置项和数据显示图表。\n" +
                "    myChart.setOption(option);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
        return template;
    }
}
