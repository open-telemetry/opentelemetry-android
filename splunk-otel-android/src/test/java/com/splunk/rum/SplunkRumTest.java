package com.splunk.rum;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class SplunkRumTest {

    @Before
    public void setUp() {
        SplunkRum.resetSingletonForTest();
    }

    @Test
    public void initialization_onlyOnce() {
        SplunkRum singleton = SplunkRum.initialize(Config.builder().beaconUrl("http://backend").rumAuthToken("abc123").build());
        SplunkRum sameInstance = SplunkRum.initialize(Config.builder().beaconUrl("http://otherbackend").rumAuthToken("123abc").build());

        assertSame(singleton, sameInstance);
    }

    @Test
    public void getInstance_preConfig() {
        assertThrows(IllegalStateException.class, SplunkRum::getInstance);
    }

    @Test
    public void getInstance() {
        SplunkRum singleton = SplunkRum.initialize(Config.builder().beaconUrl("http://backend").rumAuthToken("abc123").build());
        assertSame(singleton, SplunkRum.getInstance());
    }

    @Test
    public void newConfigBuilder() {
        assertNotNull(SplunkRum.newConfigBuilder());
    }
}