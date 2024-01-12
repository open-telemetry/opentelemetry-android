/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public interface InitializationEvents {

    void sdkInitializationStarted();

    void recordConfiguration(OtelRumConfig config);

    void currentNetworkProviderInitialized();

    void networkMonitorInitialized();

    void anrMonitorInitialized();

    void slowRenderingDetectorInitialized();

    void crashReportingInitialized();

    void spanExporterInitialized(SpanExporter spanExporter);

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

                @Override
                public void anrMonitorInitialized() {}

                @Override
                public void slowRenderingDetectorInitialized() {}

                @Override
                public void crashReportingInitialized() {}

                @Override
                public void spanExporterInitialized(SpanExporter spanExporter) {}
            };
}
