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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.location.Location;
import android.webkit.WebView;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import okhttp3.OkHttpClient;
import org.junit.Test;

public class NoOpSplunkRumTest {

    @Test
    public void doesNotThrow() {
        NoOpSplunkRum instance = NoOpSplunkRum.INSTANCE;
        instance.addRumEvent("foo", Attributes.empty());
        instance.addRumException(new RuntimeException(), Attributes.empty());

        assertNotNull(instance.createOkHttpRumInterceptor());
        assertNotNull(instance.getOpenTelemetry());
        assertNotNull(instance.getRumSessionId());
        assertNotNull(instance.getTracer());
        assertNotNull(instance.startWorkflow("foo"));
        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        assertSame(okHttpClient, instance.createRumOkHttpCallFactory(okHttpClient));

        instance.updateGlobalAttributes(attributesBuilder -> {});
        assertSame(instance, instance.setGlobalAttribute(AttributeKey.stringKey("foo"), "bar"));
        instance.flushSpans();

        instance.integrateWithBrowserRum(mock(WebView.class));

        Location location = mock(Location.class);
        instance.updateLocation(location);
    }
}
