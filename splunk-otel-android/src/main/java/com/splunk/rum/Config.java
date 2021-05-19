package com.splunk.rum;

/**
 * Configuration class for the Splunk Android RUM (Real User Monitoring) library.
 *
 * Both the beaconUrl and the rumAuthToken are mandatory configuration settings. Trying
 * to build a Config instance without both of these items specified will result in an exception being thrown.
 */
public class Config {

    private final String beaconUrl;
    private final String rumAuthToken;
    private final boolean debugEnabled;
    private final String applicationName;

    private Config(Builder builder) {
        this.beaconUrl = builder.beaconUrl;
        this.rumAuthToken = builder.rumAuthToken;
        this.debugEnabled = builder.debugEnabled;
        this.applicationName = builder.applicationName;
    }

    /**
     * The configured "beacon" URL for the RUM library.
     */
    public String getBeaconUrl() {
        return beaconUrl;
    }

    /**
     * The configured RUM auth token for the library.
     */
    public String getRumAuthToken() {
        return rumAuthToken;
    }

    /**
     * Is debug mode enabled.
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * The name under which this application will be reported to the Splunk RUM system.
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Create a new instance of the {@link Builder} class. All default configuration options will be pre-populated.
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder class for the Splunk RUM {@link Config} class.
     */
    public static class Builder {
        private String beaconUrl;
        private String rumAuthToken;
        private boolean debugEnabled = false;
        private String applicationName;

        /**
         * Create a new instance of {@link Config} from the options provided.
         */
        public Config build() {
            if (rumAuthToken == null || beaconUrl == null || applicationName == null) {
                throw new IllegalStateException("You must provide a rumAuthToken, a beaconUrl, and an application name to create a valid Config instance.");
            }
            return new Config(this);
        }

        /**
         * Assign the "beacon" URL to be used by the RUM library.
         * @return this
         */
        public Builder beaconUrl(String beaconUrl) {
            this.beaconUrl = beaconUrl;
            return this;
        }

        /**
         * Assign the RUM auth token to be used by the RUM library.
         * @return this
         */
        public Builder rumAuthToken(String rumAuthToken) {
            this.rumAuthToken = rumAuthToken;
            return this;
        }

        /**
         * Enable/disable debugging information to be emitted from the RUM library. This is set to
         * {@code false} by default.
         *
         * @return this
         */
        public Builder enableDebug(boolean enable) {
            this.debugEnabled = enable;
            return this;
        }

        /**
         * Assign an application name that will be used to identify your application in the Splunk RUM UI.
         *
         * @return this.
         */
        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }
    }
}
