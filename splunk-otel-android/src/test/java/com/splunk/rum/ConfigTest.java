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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ConfigTest {

    @Test
    public void buildingRequiredFields() {
        assertThrows(IllegalStateException.class, () -> Config.builder().build());
        assertThrows(IllegalStateException.class, () -> Config.builder().rumAccessToken("abc123").beaconEndpoint("http://backend").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().beaconEndpoint("http://backend").applicationName("appName").build());
        assertThrows(IllegalStateException.class, () -> Config.builder().applicationName("appName").rumAccessToken("abc123").build());
    }

    @Test
    public void creation() {
        Attributes globalAttributes = Attributes.of(stringKey("cheese"), "Camembert");
        Attributes expectedFinalAttributes = globalAttributes.toBuilder()
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "production")
                .build();
        Config config = Config.builder().applicationName("appName")
                .rumAccessToken("authToken")
                .beaconEndpoint("http://beacon")
                .debugEnabled(true)
                .crashReportingEnabled(false)
                .networkMonitorEnabled(false)
                .anrDetectionEnabled(false)
                .globalAttributes(globalAttributes)
                .deploymentEnvironment("production")
                .build();
        assertNotNull(config);
        assertEquals("appName", config.getApplicationName());
        assertEquals("authToken", config.getRumAccessToken());
        assertEquals("http://beacon", config.getBeaconEndpoint());
        assertTrue(config.isDebugEnabled());
        assertFalse(config.isCrashReportingEnabled());
        assertFalse(config.isNetworkMonitorEnabled());
        assertFalse(config.isAnrDetectionEnabled());
        assertEquals(expectedFinalAttributes, config.getGlobalAttributes());
    }

    @Test
    public void creation_default() {
        Config config = Config.builder().applicationName("appName")
                .rumAccessToken("authToken")
                .realm("foo")
                .build();
        assertNotNull(config);
        assertEquals("appName", config.getApplicationName());
        assertEquals("authToken", config.getRumAccessToken());
        assertEquals("https://rum-ingest.foo.signalfx.com/v1/rum", config.getBeaconEndpoint());
        assertFalse(config.isDebugEnabled());
        assertTrue(config.isCrashReportingEnabled());
        assertTrue(config.isNetworkMonitorEnabled());
        assertTrue(config.isAnrDetectionEnabled());
        assertEquals(Attributes.empty(), config.getGlobalAttributes());
    }

    @Test
    public void creation_nullHandling() {
        Config config = Config.builder().applicationName("appName")
                .rumAccessToken("authToken")
                .beaconEndpoint("http://beacon")
                .globalAttributes(null)
                .build();
        assertEquals(Attributes.empty(), config.getGlobalAttributes());
    }

    @Test
    public void updateGlobalAttributes() {
        Config config = Config.builder().applicationName("appName")
                .rumAccessToken("accessToken")
                .realm("us0")
                .globalAttributes(Attributes.of(stringKey("food"), "candy"))
                .build();

        config.updateGlobalAttributes(ab -> ab.put("drink", "lemonade"));
        Attributes result = config.getGlobalAttributes();
        assertEquals(Attributes.of(stringKey("drink"), "lemonade", stringKey("food"), "candy"), result);
    }
}