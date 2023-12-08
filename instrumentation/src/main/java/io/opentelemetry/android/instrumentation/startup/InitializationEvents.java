/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import io.opentelemetry.android.config.OtelRumConfig;

public interface InitializationEvents {

    void sdkInitializationStarted();

    void recordConfiguration(OtelRumConfig config);

    void currentNetworkProviderInitialized();

    void networkMonitorInitialized();

    InitializationEvents NO_OP =
            new InitializationEvents() {
                @Override
                public void sdkInitializationStarted() {}

                @Override
                public void recordConfiguration(OtelRumConfig config) {}

                @Override
                public void currentNetworkProviderInitialized() {}

                @Override
                public void networkMonitorInitialized() {}
            };
}
