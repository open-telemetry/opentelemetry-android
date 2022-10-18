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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import android.app.Application;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;

class SplunkRumBuilderTest {

    @Test
    void buildingRequiredFields() {
        Application app = mock(Application.class);

        assertThrows(IllegalStateException.class, () -> SplunkRum.builder().build(app));
        assertThrows(
                IllegalStateException.class,
                () ->
                        SplunkRum.builder()
                                .setRumAccessToken("abc123")
                                .setBeaconEndpoint("http://backend")
                                .build(app));
        assertThrows(
                IllegalStateException.class,
                () ->
                        SplunkRum.builder()
                                .setBeaconEndpoint("http://backend")
                                .setApplicationName("appName")
                                .build(app));
        assertThrows(
                IllegalStateException.class,
                () ->
                        SplunkRum.builder()
                                .setApplicationName("appName")
                                .setRumAccessToken("abc123")
                                .build(app));
    }

    @Test
    void defaultValues() {
        SplunkRumBuilder builder = SplunkRum.builder();

        assertFalse(builder.debugEnabled);
        assertFalse(builder.diskBufferingEnabled);
        assertTrue(builder.crashReportingEnabled);
        assertTrue(builder.networkMonitorEnabled);
        assertTrue(builder.anrDetectionEnabled);
        assertTrue(builder.slowRenderingDetectionEnabled);
        assertEquals(Attributes.empty(), builder.globalAttributes);
        assertNull(builder.deploymentEnvironment);
        assertFalse(builder.sessionBasedSamplerEnabled);
    }

    @Test
    void handleNullAttributes() {
        SplunkRumBuilder builder = SplunkRum.builder().setGlobalAttributes(null);
        assertEquals(Attributes.empty(), builder.globalAttributes);
    }

    @Test
    void setBeaconFromRealm() {
        SplunkRumBuilder builder = SplunkRum.builder().setRealm("us0");
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/rum", builder.beaconEndpoint);
    }

    @Test
    void beaconOverridesRealm() {
        SplunkRumBuilder builder =
                SplunkRum.builder().setRealm("us0").setBeaconEndpoint("http://beacon");
        assertEquals("http://beacon", builder.beaconEndpoint);
    }
}
