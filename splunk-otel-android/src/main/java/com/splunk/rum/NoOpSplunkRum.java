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

import android.webkit.WebView;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.OpenTelemetryRum;

import okhttp3.Call;
import okhttp3.OkHttpClient;

import java.util.function.Consumer;

class NoOpSplunkRum extends SplunkRum {
    static final NoOpSplunkRum INSTANCE = new NoOpSplunkRum();

    // passing null values here is fine, they'll never get used anyway
    @SuppressWarnings("NullAway")
    private NoOpSplunkRum() {
        super(OpenTelemetryRum.noop(), null);
    }

    @Override
    public Call.Factory createRumOkHttpCallFactory(OkHttpClient client) {
        return client;
    }

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return OpenTelemetry.noop();
    }

    @Override
    Tracer getTracer() {
        return getOpenTelemetry().getTracer("unused");
    }

    @Override
    public void updateGlobalAttributes(Consumer<AttributesBuilder> attributesUpdater) {
        // no-op
    }

    @Override
    public String getRumSessionId() {
        return "";
    }

    @Override
    public void addRumEvent(String name, Attributes attributes) {
        // no-op
    }

    @Override
    public void addRumException(Throwable throwable, Attributes attributes) {
        // no-op
    }

    @Override
    public void integrateWithBrowserRum(WebView webView) {
        // no-op
    }

    @Override
    void flushSpans() {
        // no-op
    }
}
