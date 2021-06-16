/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import android.app.Application;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTracing;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import okhttp3.Interceptor;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * Entrypoint for Splunk's Android RUM (Real User Monitoring) support.
 */
public class SplunkRum {
    static final AttributeKey<String> COMPONENT_KEY = AttributeKey.stringKey("component");
    static final AttributeKey<String> SCREEN_NAME_KEY = AttributeKey.stringKey("screen.name");
    static final AttributeKey<String> LAST_SCREEN_NAME_KEY = AttributeKey.stringKey("last.screen.name");
    static final String COMPONENT_APPSTART = "appstart";
    static final String COMPONENT_ERROR = "error";
    static final String COMPONENT_UI = "ui";
    static final AttributeKey<String> ERROR_TYPE_KEY = stringKey("error.type");
    static final AttributeKey<String> ERROR_MESSAGE_KEY = stringKey("error.message");

    static final String LOG_TAG = "SplunkRum";
    static final String RUM_TRACER_NAME = "SplunkRum";

    private static SplunkRum INSTANCE;

    private final Config config;
    private final SessionId sessionId;
    private final OpenTelemetrySdk openTelemetrySdk;

    SplunkRum(Config config, OpenTelemetrySdk openTelemetrySdk, SessionId sessionId) {
        this.config = config;
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionId = sessionId;
    }

    /**
     * Create a new {@link Config.Builder} instance.
     */
    public static Config.Builder newConfigBuilder() {
        return Config.builder();
    }

    /**
     * Initialized the Splunk RUM library with the provided {@link Config} instance.
     * Note: if you call this method more than once, only the first one will do anything. Repeated
     * calls will just immediately return the previously configured instance.
     *
     * @param config      The {@link Config} options to use for initialization.
     * @param application The {@link Application} to be monitored.
     * @return A fully initialized {@link SplunkRum} instance, ready for use.
     */
    public static SplunkRum initialize(Config config, Application application) {
        return initialize(config, application, () -> new ConnectionUtil(application.getApplicationContext()));
    }

    //for testing purposes
    static SplunkRum initialize(Config config, Application application, Supplier<ConnectionUtil> connectionUtilSupplier) {
        if (INSTANCE != null) {
            Log.w(LOG_TAG, "Singleton SplunkRum instance has already been initialized.");
            return INSTANCE;
        }

        INSTANCE = new RumInitializer(config, application).initialize(connectionUtilSupplier);

        if (config.isDebugEnabled()) {
            Log.i(LOG_TAG, "Splunk RUM monitoring initialized with session ID: " + INSTANCE.sessionId);
        }

        return INSTANCE;
    }

    /**
     * Get the singleton instance of this class.
     */
    public static SplunkRum getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SplunkRum has not been initialized.");
        }
        return INSTANCE;
    }

    public Interceptor createOkHttpRumInterceptor() {
        return new OkHttpRumInterceptor(OkHttpTracing.create(openTelemetrySdk).newInterceptor(), new ServerTimingHeaderParser());
    }

    /**
     * Add a custom event to RUM monitoring. This can be useful to capture business events, or
     * simply add instrumentation to your application.
     * <p>
     * This event will be turned into a Span and sent to the RUM ingest along with other, auto-generated
     * spans.
     *
     * @param name       The name of the event.
     * @param attributes Any {@link Attributes} to associate with the event.
     */
    public void addRumEvent(String name, Attributes attributes) {
        openTelemetrySdk.getTracer(RUM_TRACER_NAME)
                .spanBuilder(name)
                .setAllAttributes(attributes)
                .startSpan()
                .end();
    }

    /**
     * Add a custom exception to RUM monitoring. This can be useful for tracking custom error
     * handling in your application.
     * <p>
     * This event will be turned into a Span and sent to the RUM ingest along with other, auto-generated
     * spans.
     *
     * @param name       The name of the event.
     * @param attributes Any {@link Attributes} to associate with the event.
     * @param throwable  A {@link Throwable} associated with this event.
     */
    public void addRumException(String name, Attributes attributes, Throwable throwable) {
        Span span = openTelemetrySdk.getTracer(RUM_TRACER_NAME)
                .spanBuilder(name)
                .setAllAttributes(attributes)
                .startSpan();
        addExceptionAttributes(span, throwable);
        span.end();
    }

    static void addExceptionAttributes(Span span, Throwable e) {
        //record these here since zipkin eats the event attributes that are recorded by default.
        span.setAttribute(SemanticAttributes.EXCEPTION_TYPE, e.getClass().getSimpleName());
        span.setAttribute(SemanticAttributes.EXCEPTION_MESSAGE, e.getMessage());

        //these attributes are here to support the RUM UI/backend until it can be updated to use otel conventions.
        span.setAttribute(ERROR_TYPE_KEY, e.getClass().getSimpleName());
        span.setAttribute(ERROR_MESSAGE_KEY, e.getMessage());
    }

    //for testing only
    static void resetSingletonForTest() {
        INSTANCE = null;
    }

    //(currently) for testing only
    void flushSpans() {
        openTelemetrySdk.getSdkTracerProvider().forceFlush().join(1, TimeUnit.SECONDS);
    }
}
