package com.arloor.forwardproxy.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public enum Tracer {

    INSTANCE;

    private io.opentelemetry.api.trace.Tracer delegate;

    Tracer() {
        // 创建TracerProvider，可以自定义TraceId，spanId生成规则；采样规则；后端（jaeger,otlp,logging）
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown";
        }
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.getDefault().toBuilder().put("service.name", hostName).build())
                .addSpanProcessor(BatchSpanProcessor.builder(new LoggingSpanExporter()).build())
                .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder().setEndpoint("http://hk.gcall.me:14250").build()).build())
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                // 跨进程传播规则
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        this.delegate = openTelemetry.getTracer("http-proxy");
    }

    public static SpanBuilder spanBuilder(String s) {
        return INSTANCE.delegate.spanBuilder(s);
    }
}
