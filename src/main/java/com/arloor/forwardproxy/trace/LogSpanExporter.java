package com.arloor.forwardproxy.trace;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

public class LogSpanExporter implements SpanExporter {
    private static final Logger logger = LoggerFactory.getLogger(LogSpanExporter.class);

    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        for (SpanData spanData : collection) {
            long durationInSeconds = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000000000;
            long durationInMills = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000000;
            String time = (durationInSeconds <= 0) ? durationInMills + "ms" : durationInSeconds + "s";
            if (SpanKind.SERVER.equals(spanData.getKind())) {
                String attrs = spanData.getAttributes().asMap().entrySet().stream()
                        .map(entry -> String.format("%s=%s", entry.getKey().getKey(), entry.getValue()))
                        .collect(Collectors.joining(", "));
                logger.info("{} for {}", String.format("%8s", time), attrs);
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
