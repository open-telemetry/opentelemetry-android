/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import io.opentelemetry.android.OtelAndroidConfig;

public interface InitializationEvents {

    void sdkInitializationStarted();

    void recordConfiguration(OtelAndroidConfig config);

    void currentNetworkProviderInitialized();

    InitializationEvents NO_OP =
            new InitializationEvents() {
                @Override
                public void sdkInitializationStarted() {}

                @Override
                public void recordConfiguration(OtelAndroidConfig config) {}

                @Override
                public void currentNetworkProviderInitialized() {}
            };
}
