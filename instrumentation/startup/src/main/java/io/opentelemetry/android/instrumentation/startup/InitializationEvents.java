/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Map;

public interface InitializationEvents {

    void sdkInitializationStarted();

    void recordConfiguration(Map<String, String> config);

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
                public void recordConfiguration(Map<String, String> config) {}

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
