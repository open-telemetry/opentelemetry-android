/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import static io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import java.time.Duration;
import org.jetbrains.annotations.NotNull;

/** Entrypoint for installing the slow rendering detection instrumentation. */
@AutoService(AndroidInstrumentation.class)
public final class SlowRenderingInstrumentation implements AndroidInstrumentation {

    private static final String INSTRUMENTATION_NAME = "slowrendering";
    Duration slowRenderingDetectionPollInterval = Duration.ofSeconds(1);
    @Nullable private volatile SlowRenderListener detector = null;

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * @param interval The period that should be used for polling.
     * @return {@code this}
     */
    public SlowRenderingInstrumentation setSlowRenderingDetectionPollInterval(Duration interval) {
        if (interval.toMillis() <= 0) {
            Log.e(
                    OTEL_RUM_LOG_TAG,
                    "Invalid slowRenderingDetectionPollInterval: "
                            + interval
                            + "; must be positive");
            return this;
        }
        this.slowRenderingDetectionPollInterval = interval;
        return this;
    }

    @Override
    public void install(@NonNull InstallationContext ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(
                    OTEL_RUM_LOG_TAG,
                    "Slow/frozen rendering detection is not supported on platforms older than Android N (SDK version 24).");
            return;
        }
        if (detector != null) {
            Log.w(
                    OTEL_RUM_LOG_TAG,
                    "SlowRenderingInstrumentation skipping installation (detector already installed)");
            return;
        }

        detector =
                new SlowRenderListener(
                        ctx.getOpenTelemetry().getTracer("io.opentelemetry.slow-rendering"),
                        slowRenderingDetectionPollInterval);

        ctx.getApplication().registerActivityLifecycleCallbacks(detector);
        detector.start();
    }

    @Override
    public void uninstall(@NotNull InstallationContext ctx) {
        if (detector == null) {
            Log.w(
                    OTEL_RUM_LOG_TAG,
                    "SlowRenderingInstrumentation skipping uninstall (detector is null)");
            return;
        }
        ctx.getApplication().unregisterActivityLifecycleCallbacks(detector);
        detector.shutdown();
        detector = null;
    }

    @NonNull
    @Override
    public String getName() {
        return INSTRUMENTATION_NAME;
    }
}
