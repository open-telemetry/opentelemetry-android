/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import io.opentelemetry.android.instrumentation.common.InstrumentedApplication;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrypoint for installing the network change monitoring instrumentation.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class NetworkChangeMonitor {

    public static NetworkChangeMonitor create(CurrentNetworkProvider currentNetworkProvider) {
        return builder(currentNetworkProvider).build();
    }

    public static NetworkChangeMonitorBuilder builder(
            CurrentNetworkProvider currentNetworkProvider) {
        return new NetworkChangeMonitorBuilder(currentNetworkProvider);
    }

    private final CurrentNetworkProvider currentNetworkProvider;
    private final List<AttributesExtractor<CurrentNetwork, Void>> additionalExtractors;

    NetworkChangeMonitor(NetworkChangeMonitorBuilder builder) {
        this.currentNetworkProvider = builder.currentNetworkProvider;
        this.additionalExtractors = new ArrayList<>(builder.additionalExtractors);
    }

    /**
     * Installs the network change monitoring instrumentation on the given {@link
     * InstrumentedApplication}.
     */
    public void installOn(InstrumentedApplication instrumentedApplication) {
        NetworkApplicationListener networkApplicationListener =
                new NetworkApplicationListener(currentNetworkProvider);
        networkApplicationListener.startMonitoring(
                buildInstrumenter(instrumentedApplication.getOpenTelemetrySdk()));
        instrumentedApplication.registerApplicationStateListener(networkApplicationListener);
    }

    private Instrumenter<CurrentNetwork, Void> buildInstrumenter(OpenTelemetry openTelemetry) {
        return Instrumenter.<CurrentNetwork, Void>builder(
                        openTelemetry, "io.opentelemetry.network", network -> "network.change")
                .addAttributesExtractor(new NetworkChangeAttributesExtractor())
                .addAttributesExtractors(additionalExtractors)
                .buildInstrumenter();
    }
}
