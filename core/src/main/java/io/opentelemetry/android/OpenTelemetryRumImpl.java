/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import javax.annotation.Nullable;

final class OpenTelemetryRumImpl implements OpenTelemetryRum {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SessionProvider sessionProvider;

    OpenTelemetryRumImpl(OpenTelemetrySdk openTelemetrySdk, SessionProvider sessionProvider) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetrySdk;
    }

    @Override
    @Nullable
    public String getRumSessionId() {
        return sessionProvider.getSessionId();
    }
}
