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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTracing;
import okhttp3.Interceptor;

class NoOpSplunkRum extends SplunkRum {
    static final NoOpSplunkRum INSTANCE = new NoOpSplunkRum();

    private NoOpSplunkRum() {
        super(null, null);
    }

    @Override
    public Interceptor createOkHttpRumInterceptor() {
        return OkHttpTracing.create(OpenTelemetry.noop()).newInterceptor();
    }

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return OpenTelemetry.noop();
    }

    @Override
    public String getRumSessionId() {
        return "";
    }

    @Override
    public void addRumEvent(String name, Attributes attributes) {
        //no-op
    }

    @Override
    public void addRumException(String name, Attributes attributes, Throwable throwable) {
        //no-op
    }

    @Override
    void flushSpans() {
        //no-op
    }
}
