package com.arloor.forwardproxy.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.net.InetAddress;
import java.net.UnknownHostException;

public enum Tracer {

    INSTANCE;

    private io.opentelemetry.api.trace.Tracer delegate;

    Tracer() {
        // 创建TracerProvider，可以自定义TraceId，spanId生成规则；采样规则；后端（jaeger,otlp,logging）
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .setResource(Resource.getDefault().toBuilder().put("service.name", serviceName()).build())
                .addSpanProcessor(SimpleSpanProcessor.create(new LogSpanExporter()))
//                .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder().setEndpoint("http://hk.gcall.me:14250").build()).build())
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                // 跨进程传播规则
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                .buildAndRegisterGlobal();
        this.delegate = openTelemetry.getTracer("main");
    }

    private String serviceName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown";
        }
        return hostName;
    }

    public static SpanBuilder spanBuilder(String s) {
        return INSTANCE.delegate.spanBuilder(s);
    }

    public static void main(String[] args) throws InterruptedException {
        Span root = Tracer.spanBuilder("stream")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("class", Tracer.class.getSimpleName())
                .setAttribute("date", System.currentTimeMillis())
                .startSpan();

        try (Scope scope = root.makeCurrent()) {
            Span span1 = Tracer.spanBuilder("process1")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan();
            span1.end();
        } finally {
            root.end();
        }
        Thread.sleep(10000000);
    }
}
