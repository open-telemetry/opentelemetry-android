/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.android.internal.services.Services;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/** Generates telemetry for when the network status changes. */
@AutoService(AndroidInstrumentation.class)
public final class NetworkChangeInstrumentation implements AndroidInstrumentation {

    final List<BiConsumer<AttributesBuilder, CurrentNetwork>> additionalExtractors =
            new ArrayList<>();

    /** Adds a {@link BiConsumer} that can add Attributes about the current network. */
    public NetworkChangeInstrumentation addAttributesExtractor(
            BiConsumer<AttributesBuilder, CurrentNetwork> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    @Override
    public void install(@NonNull InstallationContext ctx) {
        additionalExtractors.add(new NetworkChangeAttributesExtractor());
        Services services = Services.get(ctx.getApplication());
        NetworkChangeMonitor networkChangeMonitor =
                new NetworkChangeMonitor(
                        ctx.getOpenTelemetry(),
                        services.getAppLifecycle(),
                        services.getCurrentNetworkProvider(),
                        Collections.unmodifiableList(additionalExtractors));
        networkChangeMonitor.start();
    }
}
