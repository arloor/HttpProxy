package com.arloor.forwardproxy.trace;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.context.Scope;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OtelContextDemo {

    /**
     * @see io.opentelemetry.api.trace.SpanContextKey
     * @see io.opentelemetry.api.baggage.BaggageContextKey
     */
    public static class XrayContextKey {
        static final ContextKey<XrayContext> KEY = ContextKey.named("xray-context-key");

        private XrayContextKey() {
        }
    }

    /**
     * implements ImplicitContextKeyed
     * @see Baggage#storeInContext(Context)
     */
    public static class XrayContext implements ImplicitContextKeyed {
        private String payload;

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public XrayContext(String payload) {
            this.payload = payload;
        }

        @Override
        public Context storeInContext(Context context) {
            return context.with(XrayContextKey.KEY, this);
        }
    }

    private static final ExecutorService poolWrapped = Context.taskWrapping(Executors.newCachedThreadPool()); // OpenTelemetry增强的线程池
    private static final ExecutorService poolUnwrapped = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Context root = Context.current().with(new XrayContext("some value")); // 设置Context
        XrayContext contextOutSideOfScope = Context.current().get(XrayContextKey.KEY);
        System.out.println("outside context is " + Optional.ofNullable(contextOutSideOfScope).map(XrayContext::getPayload).orElse(null));
        try (Scope scope = root.makeCurrent()) { // 放置到threadlocal
            XrayContext contextInScope = Context.current().get(XrayContextKey.KEY);
            System.out.println("inner context is " + Optional.ofNullable(contextInScope).map(XrayContext::getPayload).orElse(null));
            poolWrapped.execute(() -> {
                XrayContext xrayContext = Context.current().get(XrayContextKey.KEY);
                System.out.println("pool wrapped context is " + Optional.ofNullable(xrayContext).map(XrayContext::getPayload).orElse(null));
            });
            poolUnwrapped.execute(() -> {
                XrayContext xrayContext = Context.current().get(XrayContextKey.KEY);
                System.out.println("pool unwrapped context is " + Optional.ofNullable(xrayContext).map(XrayContext::getPayload).orElse(null));
            });
        }
        try {
            poolUnwrapped.shutdown();
            poolWrapped.shutdown();
            poolUnwrapped.awaitTermination(1, TimeUnit.SECONDS);
            poolWrapped.awaitTermination(1, TimeUnit.SECONDS);
        } catch (Throwable e) {
            poolUnwrapped.shutdownNow();
            poolWrapped.shutdownNow();
        }
    }
}
