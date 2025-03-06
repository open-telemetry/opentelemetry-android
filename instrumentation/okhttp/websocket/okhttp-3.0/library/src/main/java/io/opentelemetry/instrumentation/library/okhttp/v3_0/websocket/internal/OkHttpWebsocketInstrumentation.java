/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal;

import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;

@AutoService(AndroidInstrumentation.class)
public class OkHttpWebsocketInstrumentation implements AndroidInstrumentation {
    @Override
    public void install(@NonNull InstallationContext ctx) {
        WebsocketEventGenerator.configure(ctx);
    }
}
