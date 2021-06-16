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

import org.junit.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        Attributes globalAttributes = Attributes.of(AttributeKey.stringKey("cheese"), "Camembert");
        Config config = Config.builder().applicationName("appName")
                .rumAuthToken("authToken")
                .beaconUrl("http://beacon")
                .debugEnabled(true)
                .crashReportingEnabled(false)
                .networkMonitorEnabled(false)
                .globalAttributes(globalAttributes)
                .build();
        assertNotNull(config);
        assertEquals("appName", config.getApplicationName());
        assertEquals("authToken", config.getRumAuthToken());
        assertEquals("http://beacon", config.getBeaconUrl());
        assertTrue(config.isDebugEnabled());
        assertFalse(config.isCrashReportingEnabled());
        assertFalse(config.isNetworkMonitorEnabled());
        assertEquals(globalAttributes, config.getGlobalAttributes());
    }

    @Test
    public void creation_default() {
        Config config = Config.builder().applicationName("appName")
                .rumAuthToken("authToken")
                .beaconUrl("http://beacon")
                .build();
        assertNotNull(config);
        assertEquals("appName", config.getApplicationName());
        assertEquals("authToken", config.getRumAuthToken());
        assertEquals("http://beacon", config.getBeaconUrl());
        assertFalse(config.isDebugEnabled());
        assertTrue(config.isCrashReportingEnabled());
        assertTrue(config.isNetworkMonitorEnabled());
        assertEquals(Attributes.empty(), config.getGlobalAttributes());
    }

    @Test
    public void creation_nullHandling() {
        Config config = Config.builder().applicationName("appName")
                .rumAuthToken("authToken")
                .beaconUrl("http://beacon")
                .globalAttributes(null)
                .build();
        assertEquals(Attributes.empty(), config.getGlobalAttributes());
    }
}