/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import android.app.Application;
import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService;
import io.opentelemetry.android.internal.services.network.CurrentNetworkService;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.ArrayList;
import java.util.List;

@AutoService(AndroidInstrumentation.class)
public final class NetworkChangeMonitorInstrumentation implements AndroidInstrumentation {

    final List<AttributesExtractor<CurrentNetwork, Void>> additionalExtractors = new ArrayList<>();

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public NetworkChangeMonitorInstrumentation addAttributesExtractor(
            AttributesExtractor<CurrentNetwork, Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    private Instrumenter<CurrentNetwork, Void> buildInstrumenter(OpenTelemetry openTelemetry) {
        return Instrumenter.<CurrentNetwork, Void>builder(
                        openTelemetry, "io.opentelemetry.network", network -> "network.change")
                .addAttributesExtractor(new NetworkChangeAttributesExtractor())
                .addAttributesExtractors(additionalExtractors)
                .buildInstrumenter();
    }

    @Override
    public void apply(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        NetworkApplicationListener networkApplicationListener =
                new NetworkApplicationListener(
                        ServiceManager.get().getService(CurrentNetworkService.class));
        networkApplicationListener.startMonitoring(
                buildInstrumenter(openTelemetryRum.getOpenTelemetry()));
        ServiceManager.get()
                .getService(AppLifecycleService.class)
                .registerListener(networkApplicationListener);
    }
}
