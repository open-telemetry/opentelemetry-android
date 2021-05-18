package com.splunk.rum;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ConfigTest {

    @Test
    public void buildingRequiredFields() {
        assertThrows(IllegalStateException.class, () -> Config.builder().build());
        assertThrows(IllegalStateException.class, () -> Config.builder().rumAuthToken("abc123").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().beaconUrl("http://backend").build());
    }
}