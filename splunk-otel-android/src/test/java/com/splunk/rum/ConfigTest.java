package com.splunk.rum;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ConfigTest {

    @Test
    public void buildingRequiredFields() {
        assertThrows(IllegalStateException.class, () -> Config.builder().build());
        assertThrows(IllegalStateException.class, () -> Config.builder().rumAuthToken("abc123").beaconUrl("http://backend").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().beaconUrl("http://backend").applicationName("appName").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().applicationName("appName").rumAuthToken("abc123").build());
    }

    @Test
    public void creation() {
        Config config = Config.builder().applicationName("appName")
                .rumAuthToken("authToken")
                .beaconUrl("http://beacon")
                .enableDebug(true)
                .build();
        assertNotNull(config);
        assertEquals("appName", config.getApplicationName());
        assertEquals("authToken", config.getRumAuthToken());
        assertEquals("http://beacon", config.getBeaconUrl());
        assertTrue(config.isDebugEnabled());
    }
}