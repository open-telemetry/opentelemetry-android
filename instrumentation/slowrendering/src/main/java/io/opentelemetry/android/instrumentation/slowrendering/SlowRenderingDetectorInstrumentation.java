/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import android.app.Application;
import androidx.annotation.NonNull;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import java.time.Duration;

/**
 * A builder of {@link SlowRenderingDetector}.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class SlowRenderingDetectorInstrumentation implements AndroidInstrumentation {

    Duration slowRenderingDetectionPollInterval = Duration.ofSeconds(1);

    @Override
    public void apply(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        SlowRenderingDetector slowRenderingDetector =
                new SlowRenderingDetector(slowRenderingDetectionPollInterval);
        slowRenderingDetector.install(application, openTelemetryRum);
    }
}
