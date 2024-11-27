/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.networkattrs;

import io.opentelemetry.android.common.internal.features.networkattrs.CurrentNetworkAttributesExtractor;
import io.opentelemetry.android.common.internal.features.networkattrs.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
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
