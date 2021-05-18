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

    private Config(Builder builder) {
        this.beaconUrl = builder.beaconUrl;
        this.rumAuthToken = builder.rumAuthToken;
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

        /**
         * Create a new instance of {@link Config} from the options provided.
         */
        public Config build() {
            if (rumAuthToken == null || beaconUrl == null) {
                throw new IllegalStateException("You must provide both a rumAuthToken and a beaconUrl to create a valid Config instance.");
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
    }
}
