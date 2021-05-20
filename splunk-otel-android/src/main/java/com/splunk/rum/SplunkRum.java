package com.splunk.rum;

import android.app.Application;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.sdk.OpenTelemetrySdk;

/**
 * Entrypoint for Splunk's Android RUM (Real User Monitoring) support.
 */
public class SplunkRum {

    private static final String LOG_TAG = "SplunkRum";

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
        if (INSTANCE != null) {
            Log.w(LOG_TAG, "Singleton SplunkRum instance has already been initialized.");
            return INSTANCE;
        }

        INSTANCE = new RumInitializer(config, application).initialize();

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

    //(currently) for testing only
    void flushSpans() {
        openTelemetrySdk.getSdkTracerProvider().forceFlush().join(1, TimeUnit.SECONDS);
    }
}
