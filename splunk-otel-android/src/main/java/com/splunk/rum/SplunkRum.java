package com.splunk.rum;

import android.util.Log;

/**
 * Entrypoint for Splunk's Android RUM (Real User Monitoring) support.
 */
public class SplunkRum {

    private static final String LOG_TAG = "SplunkRum";

    private static SplunkRum INSTANCE;

    private final Config config;

    private SplunkRum(Config config) {
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
     * @param config The {@link Config} options to use for initialization.
     * @return A fully initialized {@link SplunkRum} instance, ready for use.
     */
    public static SplunkRum initialize(Config config) {
        if (INSTANCE != null) {
            Log.w(LOG_TAG, "Singleton SplunkRum instance has already been initialized.");
            return INSTANCE;
        }
        INSTANCE = new SplunkRum(config);
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
