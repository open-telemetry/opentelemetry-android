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

package io.opentelemetry.rum.internal.instrumentation.network;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * A {@link SpanProcessor} implementation that appends a set of {@linkplain Attributes attributes}
 * describing the {@linkplain CurrentNetwork current network} to every span that is exported.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class NetworkAttributesSpanAppender implements SpanProcessor {

    public static SpanProcessor create(CurrentNetworkProvider currentNetworkProvider) {
        return new NetworkAttributesSpanAppender(currentNetworkProvider);
    }

    private final CurrentNetworkProvider currentNetworkProvider;
    private final CurrentNetworkAttributesExtractor networkAttributesExtractor =
            new CurrentNetworkAttributesExtractor();

    NetworkAttributesSpanAppender(CurrentNetworkProvider currentNetworkProvider) {
        this.currentNetworkProvider = currentNetworkProvider;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        CurrentNetwork currentNetwork = currentNetworkProvider.getCurrentNetwork();
        span.setAllAttributes(networkAttributesExtractor.extract(currentNetwork));
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
