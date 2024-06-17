/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.List;

/**
 * Entrypoint for installing the network change monitoring instrumentation.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class NetworkChangeMonitor {
    private final OpenTelemetry openTelemetry;
    private final AppLifecycleService appLifecycleService;

    public NetworkChangeMonitor(
            OpenTelemetry openTelemetry,
            AppLifecycleService appLifecycleService,
            CurrentNetworkProvider currentNetworkProvider,
            List<AttributesExtractor<CurrentNetwork, Void>> additionalExtractors) {
        this.openTelemetry = openTelemetry;
        this.appLifecycleService = appLifecycleService;
        this.currentNetworkProvider = currentNetworkProvider;
        this.additionalExtractors = additionalExtractors;
    }

    private final CurrentNetworkProvider currentNetworkProvider;
    private final List<AttributesExtractor<CurrentNetwork, Void>> additionalExtractors;

    /**
     * Installs the network change monitoring instrumentation on the given {@link OpenTelemetry}.
     */
    public void start() {
        NetworkApplicationListener networkApplicationListener =
                new NetworkApplicationListener(currentNetworkProvider);
        networkApplicationListener.startMonitoring(buildInstrumenter(openTelemetry));
        appLifecycleService.registerListener(networkApplicationListener);
    }

    private Instrumenter<CurrentNetwork, Void> buildInstrumenter(OpenTelemetry openTelemetry) {
        return Instrumenter.<CurrentNetwork, Void>builder(
                        openTelemetry, "io.opentelemetry.network", network -> "network.change")
                .addAttributesExtractor(new NetworkChangeAttributesExtractor())
                .addAttributesExtractors(additionalExtractors)
                .buildInstrumenter();
    }
}
