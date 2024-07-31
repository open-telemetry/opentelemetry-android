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
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Generates telemetry for when the network status changes. */
@AutoService(AndroidInstrumentation.class)
public final class NetworkChangeInstrumentation implements AndroidInstrumentation {

    final List<AttributesExtractor<CurrentNetwork, Void>> additionalExtractors = new ArrayList<>();

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public NetworkChangeInstrumentation addAttributesExtractor(
            AttributesExtractor<CurrentNetwork, Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    @Override
    public void install(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        NetworkChangeMonitor networkChangeMonitor =
                new NetworkChangeMonitor(
                        openTelemetryRum.getOpenTelemetry(),
                        ServiceManager.get().getAppLifecycleService(),
                        ServiceManager.get().getCurrentNetworkProvider(),
                        Collections.unmodifiableList(additionalExtractors));
        networkChangeMonitor.start();
    }
}
