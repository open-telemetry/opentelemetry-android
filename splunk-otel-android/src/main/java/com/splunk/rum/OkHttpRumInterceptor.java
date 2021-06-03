package com.splunk.rum;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * Note: When the the okhttp3 interceptor from otel has been converted to use the new Instrumentation APIs,
 * we'll be able to do this much more simply without all this craziness just by giving the otel interceptor
 * a response handler implementation.
 */
class OkHttpRumInterceptor implements Interceptor {
    static final AttributeKey<String> LINK_TRACE_ID_KEY = stringKey("link.traceId");
    static final AttributeKey<String> LINK_SPAN_ID_KEY = stringKey("link.spanId");
    static final AttributeKey<String> ERROR_TYPE_KEY = stringKey("error.type");
    static final AttributeKey<String> ERROR_MESSAGE_KEY = stringKey("error.message");

    private final Interceptor coreInterceptor;
    private final ServerTimingHeaderParser headerParser;

    public OkHttpRumInterceptor(Interceptor coreInterceptor, ServerTimingHeaderParser headerParser) {
        this.coreInterceptor = coreInterceptor;
        this.headerParser = headerParser;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return coreInterceptor.intercept(new DelegatingChain(chain, headerParser));
    }

    /**
     * A {@link okhttp3.Interceptor.Chain} implementation that allows delegating to another interceptor.
     */
    private static class DelegatingChain implements Chain {
        private final Chain chain;
        private final ServerTimingHeaderParser headerParser;

        public DelegatingChain(Chain chain, ServerTimingHeaderParser headerParser) {
            this.chain = chain;
            this.headerParser = headerParser;
        }

        @NotNull
        @Override
        public Request request() {
            return chain.request();
        }

        @NotNull
        @Override
        public Response proceed(@NotNull Request request) throws IOException {
            Span span = Span.current();
            span.setAttribute(SplunkRum.COMPONENT_KEY, "http");

            Response response = null;
            try {
                response = chain.proceed(request);
            } catch (IOException e) {
                //record these here since zipkin eats the event attributes that are recorded by default.
                span.setAttribute(SemanticAttributes.EXCEPTION_TYPE, e.getClass().getSimpleName());
                span.setAttribute(SemanticAttributes.EXCEPTION_MESSAGE, e.getMessage());

                //these attributes are here to support the RUM UI/backend until it can be updated to use otel conventions.
                span.setAttribute(ERROR_TYPE_KEY, e.getClass().getSimpleName());
                span.setAttribute(ERROR_MESSAGE_KEY, e.getMessage());
                throw e;
            }

            String serverTimingHeader = response.header("Server-Timing");

            String[] ids = headerParser.parse(serverTimingHeader);
            if (ids.length == 2) {
                span.setAttribute(LINK_TRACE_ID_KEY, ids[0]);
                span.setAttribute(LINK_SPAN_ID_KEY, ids[1]);
            }
            return response;
        }

        @Override
        public Connection connection() {
            return chain.connection();
        }

        @NotNull
        @Override
        public Call call() {
            return chain.call();
        }

        @Override
        public int connectTimeoutMillis() {
            return chain.connectTimeoutMillis();
        }

        @NotNull
        @Override
        public Chain withConnectTimeout(int i, @NotNull TimeUnit timeUnit) {
            return chain.withConnectTimeout(i, timeUnit);
        }

        @Override
        public int readTimeoutMillis() {
            return chain.readTimeoutMillis();
        }

        @NotNull
        @Override
        public Chain withReadTimeout(int i, @NotNull TimeUnit timeUnit) {
            return chain.withReadTimeout(i, timeUnit);
        }

        @Override
        public int writeTimeoutMillis() {
            return chain.writeTimeoutMillis();
        }

        @NotNull
        @Override
        public Chain withWriteTimeout(int i, @NotNull TimeUnit timeUnit) {
            return chain.withWriteTimeout(i, timeUnit);
        }
    }
}
