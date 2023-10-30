/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import io.opentelemetry.android.OtelRumConfig;

public class SdkInitializationEvents implements InitializationEvents {

    @Override
    public void sdkInitializationStarted() {
        // TODO: Build me
    }

    @Override
    public void recordConfiguration(OtelRumConfig config) {
        // TODO: Build me (create event containing the config params for the sdk)
    }

    @Override
    public void currentNetworkProviderInitialized() {
        // TODO: Build me
    }
}
