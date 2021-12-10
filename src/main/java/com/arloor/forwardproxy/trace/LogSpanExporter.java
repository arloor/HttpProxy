package com.arloor.forwardproxy.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class LogSpanExporter implements SpanExporter {
    private static final Logger logger = LoggerFactory.getLogger(Tracer.class);

    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        for (SpanData spanData : collection) {
            long durationInSeconds = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000000000;
            long durationInMills = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000000;
            String time = (durationInSeconds <= 0) ? durationInMills + "ms" : durationInSeconds + "s";
            String name = spanData.getName();
            if (TraceConstant.stream.name().equals(name)) {
                String host = spanData.getAttributes().get(AttributeKey.stringKey(TraceConstant.host.name()));
                logger.info("{} lifetime is {}", host, time);
            }
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
