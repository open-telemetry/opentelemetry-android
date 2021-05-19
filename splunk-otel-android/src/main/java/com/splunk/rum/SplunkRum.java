package com.splunk.rum;

import android.app.Application;
import android.util.Log;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Entrypoint for Splunk's Android RUM (Real User Monitoring) support.
 */
public class SplunkRum {

    private static final String LOG_TAG = "SplunkRum";

    private static SplunkRum INSTANCE;

    private final Config config;
    private final SessionId sessionId;
    private final OpenTelemetrySdk openTelemetrySdk;

    private SplunkRum(Config config, OpenTelemetrySdk openTelemetrySdk, SessionId sessionId) {
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
        if (INSTANCE != null) {
            Log.w(LOG_TAG, "Singleton SplunkRum instance has already been initialized.");
            return INSTANCE;
        }
        String endpoint = config.getBeaconUrl() + "?auth=" + config.getRumAuthToken();
        SpanExporter zipkinExporter = ZipkinSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
        SessionId sessionId = new SessionId();
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(zipkinExporter).build())
                .addSpanProcessor(new RumAttributeAppender(config, sessionId))
                .setResource(Resource.getDefault().toBuilder().put("service.name", config.getApplicationName()).build())
                .build();
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .build();

        application.registerActivityLifecycleCallbacks(new RumLifecycleCallbacks(openTelemetrySdk.getTracer("SplunkRum")));

        INSTANCE = new SplunkRum(config, openTelemetrySdk, sessionId);
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

    //for testing only
    static void resetSingletonForTest() {
        INSTANCE = null;
    }
}
