/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.sdk.trace.export.SpanExporter;

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

    @Override
    public void networkMonitorInitialized() {
        // TODO: Build me "networkMonitorInitialized"
    }

    @Override
    public void anrMonitorInitialized() {
        // TODO: Build me "anrMonitorInitialized"
    }

    @Override
    public void slowRenderingDetectorInitialized() {
        // TODO: Build me "slowRenderingDetectorInitialized"
    }

    @Override
    public void crashReportingInitialized() {
        // TODO: Build me "crashReportingInitialized"
    }

    @Override
    public void spanExporterInitialized(SpanExporter spanExporter) {
        // TODO: Build me "spanExporterInitialized"
    }
}
