/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.networkattrs;

import io.opentelemetry.android.internal.services.network.CurrentNetworkService;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
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

    public static SpanProcessor create(CurrentNetworkService currentNetworkService) {
        return new NetworkAttributesSpanAppender(currentNetworkService);
    }

    private final CurrentNetworkService currentNetworkService;
    private final CurrentNetworkAttributesExtractor networkAttributesExtractor =
            new CurrentNetworkAttributesExtractor();

    NetworkAttributesSpanAppender(CurrentNetworkService currentNetworkService) {
        this.currentNetworkService = currentNetworkService;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        CurrentNetwork currentNetwork = currentNetworkService.getCurrentNetwork();
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
