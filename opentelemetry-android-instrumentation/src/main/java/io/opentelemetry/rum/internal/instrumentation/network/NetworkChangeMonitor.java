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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
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
