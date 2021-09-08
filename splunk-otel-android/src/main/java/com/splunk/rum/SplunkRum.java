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

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTracing;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Entrypoint for Splunk's Android RUM (Real User Monitoring) support.
 */
public class SplunkRum {
    //initialize this here, statically, to make sure we capture the earliest possible timestamp for startup.
    private static final AppStartupTimer startupTimer = new AppStartupTimer();

    static final AttributeKey<String> COMPONENT_KEY = AttributeKey.stringKey("component");
    static final AttributeKey<String> SCREEN_NAME_KEY = AttributeKey.stringKey("screen.name");
    static final AttributeKey<String> LAST_SCREEN_NAME_KEY = AttributeKey.stringKey("last.screen.name");
    static final AttributeKey<String> ERROR_TYPE_KEY = stringKey("error.type");
    static final AttributeKey<String> ERROR_MESSAGE_KEY = stringKey("error.message");
    static final AttributeKey<String> WORKFLOW_NAME_KEY = stringKey("workflow.name");
    static final AttributeKey<String> START_TYPE_KEY = stringKey("start.type");

    static final String COMPONENT_APPSTART = "appstart";
    static final String COMPONENT_CRASH = "crash";
    static final String COMPONENT_ERROR = "error";
    static final String COMPONENT_UI = "ui";
    static final String LOG_TAG = "SplunkRum";
    static final String RUM_TRACER_NAME = "SplunkRum";

    private static SplunkRum INSTANCE;

    private final SessionId sessionId;
    private final OpenTelemetrySdk openTelemetrySdk;
    private final Config config;

    SplunkRum(OpenTelemetrySdk openTelemetrySdk, SessionId sessionId, Config config) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionId = sessionId;
        this.config = config;
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
        return initialize(config, application, () -> {
            Context context = application.getApplicationContext();
            ConnectionUtil connectionUtil = new ConnectionUtil(NetworkDetector.create(context));
            connectionUtil.startMonitoring(ConnectionUtil::createNetworkMonitoringRequest, (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
            return connectionUtil;
        });
    }

    //for testing purposes
    static SplunkRum initialize(Config config, Application application, Supplier<ConnectionUtil> connectionUtilSupplier) {
        if (INSTANCE != null) {
            Log.w(LOG_TAG, "Singleton SplunkRum instance has already been initialized.");
            return INSTANCE;
        }

        INSTANCE = new RumInitializer(config, application, startupTimer)
                .initialize(connectionUtilSupplier, Looper.getMainLooper());

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
            Log.d(LOG_TAG, "SplunkRum not initialized. Returning no-op implementation");
            return NoOpSplunkRum.INSTANCE;
        }
        return INSTANCE;
    }

    /**
     * Create an OkHttp3 {@link Interceptor} configured with the OpenTelemetry instance backing this
     * class. It will provide both standard OpenTelemetry spans and additionally Splunk RUM-specific
     * attributes.
     *
     * @deprecated The OpenTelemetry {@link Interceptor} has been deprecated in favor of using an instrumented
     * {@link okhttp3.Call.Factory} implementation. Please use {@link #createRumOkHttpCallFactory(OkHttpClient)}.
     */
    @Deprecated
    public Interceptor createOkHttpRumInterceptor() {
        Interceptor coreInterceptor = createOkHttpTracing().newInterceptor();
        return new OkHttpRumInterceptor(coreInterceptor);
    }

    /**
     * Wrap the provided {@link OkHttpClient} with OpenTelemetry and RUM instrumentation. Since
     * {@link Call.Factory} is the primary useful interface implemented by the OkHttpClient, this
     * should be a drop-in replacement for any usages of OkHttpClient.
     *
     * @param client The {@link OkHttpClient} to wrap with OpenTelemetry and RUM instrumentation.
     * @return A {@link okhttp3.Call.Factory} implementation.
     */
    public Call.Factory createRumOkHttpCallFactory(OkHttpClient client) {
        return createOkHttpTracing().newCallFactory(client);
    }

    private OkHttpTracing createOkHttpTracing() {
        return OkHttpTracing
                .newBuilder(openTelemetrySdk)
                .addAttributesExtractor(new RumResponseAttributesExtractor(new ServerTimingHeaderParser()))
                .build();
    }

    /**
     * Get a handle to the instance of the OpenTelemetry API that this instance is using for instrumentation.
     */
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetrySdk;
    }

    /**
     * Get the Splunk Session ID associated with this instance of the RUM instrumentation library.
     * Note: this value can change throughout the lifetime of an application instance, so it
     * is recommended that you do not cache this value, but always retrieve it from here when
     * needed.
     */
    public String getRumSessionId() {
        return sessionId.getSessionId();
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
        getTracer()
                .spanBuilder(name)
                .setAllAttributes(attributes)
                .startSpan()
                .end();
    }

    /**
     * Start a Span to time a named workflow.
     *
     * @param workflowName The name of the workflow to start.
     * @return A {@link Span} that has been started.
     */
    public Span startWorkflow(String workflowName) {
        return getTracer()
                .spanBuilder(workflowName)
                .setAttribute(WORKFLOW_NAME_KEY, workflowName)
                .startSpan();
    }

    /**
     * Add a custom exception to RUM monitoring. This can be useful for tracking custom error
     * handling in your application.
     * <p>
     * This event will be turned into a Span and sent to the RUM ingest along with other, auto-generated
     * spans.
     *
     * @param throwable A {@link Throwable} associated with this event.
     */
    public void addRumException(Throwable throwable) {
        addRumException(throwable, Attributes.empty());
    }

    /**
     * Add a custom exception to RUM monitoring. This can be useful for tracking custom error
     * handling in your application.
     * <p>
     * This event will be turned into a Span and sent to the RUM ingest along with other, auto-generated
     * spans.
     *
     * @param throwable  A {@link Throwable} associated with this event.
     * @param attributes Any {@link Attributes} to associate with the event.
     */
    public void addRumException(Throwable throwable, Attributes attributes) {
        Span span = getTracer()
                .spanBuilder(throwable.getClass().getSimpleName())
                .setAllAttributes(attributes)
                .setAttribute(COMPONENT_KEY, COMPONENT_ERROR)
                .startSpan();
        addExceptionAttributes(span, throwable);
        span.end();
    }

    Tracer getTracer() {
        return openTelemetrySdk.getTracer(RUM_TRACER_NAME);
    }

    static void addExceptionAttributes(Span span, Throwable e) {
        //record these here since zipkin eats the event attributes that are recorded by default.
        span.setAttribute(SemanticAttributes.EXCEPTION_TYPE, e.getClass().getSimpleName());
        span.setAttribute(SemanticAttributes.EXCEPTION_MESSAGE, e.getMessage());

        //these attributes are here to support the RUM UI/backend until it can be updated to use otel conventions.
        span.setAttribute(ERROR_TYPE_KEY, e.getClass().getSimpleName());
        span.setAttribute(ERROR_MESSAGE_KEY, e.getMessage());
    }

    void recordAnr(StackTraceElement[] stackTrace) {
        getTracer()
                .spanBuilder("ANR")
                .setAttribute(SemanticAttributes.EXCEPTION_STACKTRACE, formatStackTrace(stackTrace))
                .setAttribute(COMPONENT_KEY, COMPONENT_ERROR)
                .startSpan()
                .end();
    }

    private String formatStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stringBuilder.append(stackTraceElement).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Set an attribute in the global attributes that will be appended to every span and event.
     * <p>
     * Note: If this key is the same as an existing key in the global attributes, it will replace the
     * existing value.
     * <p>
     * If you attempt to set a value to null or use a null key, this call will be ignored.
     * <p>
     * Note: If multiple concurrent calls are made to this, the resulting set of attributes will
     * only reflect one of the updates, and which one wins is non-deterministic.
     *
     * @param key   The {@link AttributeKey} for the attribute.
     * @param value The value of the attribute, which must match the generic type of the key.
     * @param <T>   The generic type of the value.
     * @return this.
     */
    public <T> SplunkRum setGlobalAttribute(AttributeKey<T> key, T value) {
        updateGlobalAttributes(attributesBuilder -> attributesBuilder.put(key, value));
        return this;
    }

    /**
     * Update the global set of attributes that will be appended to every span and event.
     * <p>
     * Note: If multiple concurrent calls are made to this, the resulting set of attributes will
     * only reflect one of the updates, and which one wins is non-deterministic.
     *
     * @param attributesUpdater A function which will update the current set of attributes, by operating on a {@link AttributesBuilder} from the current set.
     */
    public void updateGlobalAttributes(Consumer<AttributesBuilder> attributesUpdater) {
        config.updateGlobalAttributes(attributesUpdater);
    }

    //for testing only
    static void resetSingletonForTest() {
        INSTANCE = null;
    }

    //(currently) for testing only
    void flushSpans() {
        openTelemetrySdk.getSdkTracerProvider().forceFlush().join(1, TimeUnit.SECONDS);
    }

    /**
     * Initialize a no-op version of the SplunkRum API, including the instance of OpenTelemetry that
     * is available. This can be useful for testing, or configuring your app without RUM enabled,
     * but still using the APIs.
     *
     * @return A no-op instance of {@link SplunkRum}
     */
    public static SplunkRum noop() {
        return NoOpSplunkRum.INSTANCE;
    }
}
