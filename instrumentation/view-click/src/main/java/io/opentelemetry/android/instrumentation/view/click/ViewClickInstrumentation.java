/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click;

import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;

@AutoService(AndroidInstrumentation.class)
public class ViewClickInstrumentation implements AndroidInstrumentation {
    public static final String INSTRUMENTATION_NAME = "android.view.click";

    @Override
    public void install(@NonNull InstallationContext ctx) {
        ctx.getApplication().registerActivityLifecycleCallbacks(new ViewClickActivityCallback());
        EventBuilderCreator.configure(ctx);
    }

    @NonNull
    @Override
    public String getName() {
        return INSTRUMENTATION_NAME;
    }
}
