package com.splunk.rum;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ConfigTest {

    @Test
    public void buildingRequiredFields() {
        assertThrows(IllegalStateException.class, () -> Config.builder().build());
        assertThrows(IllegalStateException.class, () -> Config.builder().rumAuthToken("abc123").beaconUrl("http://backend").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().beaconUrl("http://backend").applicationName("appName").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().applicationName("appName").rumAuthToken("abc123").build());
    }
}