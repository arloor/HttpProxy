package com.arloor.forwardproxy.monitor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.opentelemetry.api.trace.Span;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ChannelTrafficMonitor extends ChannelTrafficShapingHandler {
    private static String[] array = {"B", "KB", "MB", "GB"};
    private final Span streamSapn;

    public ChannelTrafficMonitor(int checkInterval, Span streamSpan) {
        super(checkInterval);
        this.streamSapn = streamSpan;
    }

    public long getReadBytes() {
        return this.trafficCounter().cumulativeReadBytes();
    }

    public long getWriteBytes() {
        return this.trafficCounter().cumulativeWrittenBytes();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        streamSapn.setAttribute("in", format(getReadBytes()));
        streamSapn.setAttribute("out", format(getWriteBytes()));
    }

    private static String format(long bytes) {
        double value = bytes;

        int index = 0;
        for (double i = value; i >= 1024 && index < array.length - 1; i /= 1024, index++, value = i) {
        }
        BigDecimal bigDecimal = new BigDecimal(value);
        if (index == array.length - 1) {
            bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
        }else {
            bigDecimal=bigDecimal.setScale(0,RoundingMode.HALF_UP);
        }
        String string = bigDecimal.toString();
        if (string.endsWith(".00")) {
            string = string.substring(0, string.length() - 3);
        }
        return string + array[index];
    }

    public static void main(String[] args) {
        System.out.println(format(1023));
        System.out.println(format(1024));
        System.out.println(format(1024 * 1024));
        System.out.println(format(1064 * 1024));
        System.out.println(format(1064 * 1024 * 1024));
        System.out.println(format(1064L * 1024 * 1024 * 1024));
    }
}
