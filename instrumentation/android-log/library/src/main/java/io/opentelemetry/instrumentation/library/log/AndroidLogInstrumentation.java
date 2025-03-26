/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log;

import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator;

@AutoService(AndroidInstrumentation.class)
public class AndroidLogInstrumentation implements AndroidInstrumentation {
    public static final String INSTRUMENTATION_NAME = "android-log";

    @Override
    public void install(@NonNull InstallationContext ctx) {
        LogRecordBuilderCreator.configure(ctx);
    }

    @NonNull
    @Override
    public String getName() {
        return INSTRUMENTATION_NAME;
    }
}
