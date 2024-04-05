/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import io.opentelemetry.android.features.networkattrs.CurrentNetwork;
import io.opentelemetry.android.features.networkattrs.CurrentNetworkProvider;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.List;

/**
 * A builder of {@link NetworkChangeMonitor}.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class NetworkChangeMonitorBuilder {

    final CurrentNetworkProvider currentNetworkProvider;
    final List<AttributesExtractor<CurrentNetwork, Void>> additionalExtractors = new ArrayList<>();

    public NetworkChangeMonitorBuilder(CurrentNetworkProvider currentNetworkProvider) {
        this.currentNetworkProvider = currentNetworkProvider;
    }

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public NetworkChangeMonitorBuilder addAttributesExtractor(
            AttributesExtractor<CurrentNetwork, Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    public NetworkChangeMonitor build() {
        return new NetworkChangeMonitor(this);
    }
}
