/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Logger;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Entrypoint for installing the network change monitoring instrumentation.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class NetworkChangeMonitor {
    private final OpenTelemetry openTelemetry;
    private final AppLifecycle appLifecycle;
    private final CurrentNetworkProvider currentNetworkProvider;
    private final List<BiConsumer<AttributesBuilder, CurrentNetwork>> additionalExtractors;

    public NetworkChangeMonitor(
            OpenTelemetry openTelemetry,
            AppLifecycle appLifecycle,
            CurrentNetworkProvider currentNetworkProvider,
            List<BiConsumer<AttributesBuilder, CurrentNetwork>> additionalExtractors) {
        this.openTelemetry = openTelemetry;
        this.appLifecycle = appLifecycle;
        this.currentNetworkProvider = currentNetworkProvider;
        this.additionalExtractors = additionalExtractors;
    }

    /**
     * Installs the network change monitoring instrumentation on the given {@link OpenTelemetry}.
     */
    public void start() {
        NetworkApplicationListener networkApplicationListener =
                new NetworkApplicationListener(currentNetworkProvider);

        Logger logger = openTelemetry.getLogsBridge().get("io.opentelemetry.network");
        networkApplicationListener.startMonitoring(logger, additionalExtractors);
        appLifecycle.registerListener(networkApplicationListener);
    }
}
