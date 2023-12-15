/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config;

import io.opentelemetry.android.ScreenAttributesSpanProcessor;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Configuration object for OpenTelemetry Android. The configuration items in this class will be
 * used in the OpenTelemetryRumBuilder to wire up and enable/disable various mobile instrumentation
 * components.
 */
public class OtelRumConfig {

    private static final Duration DEFAULT_SLOW_RENDERING_DETECTION_POLL_INTERVAL =
            Duration.ofSeconds(1);

    private Supplier<Attributes> globalAttributesSupplier = Attributes::empty;
    private boolean includeNetworkAttributes = true;
    private boolean generateSdkInitializationEvents = true;
    private boolean includeScreenAttributes = true;
    private DiskBufferingConfiguration diskBufferingConfiguration =
            DiskBufferingConfiguration.builder().build();
    private boolean networkChangeMonitoringEnabled = true;
    private boolean anrDetectionEnabled = true;
    private boolean slowRenderingDetectionEnabled = true;
    private Duration slowRenderingDetectionPollInterval =
            DEFAULT_SLOW_RENDERING_DETECTION_POLL_INTERVAL;

    /**
     * Configures the set of global attributes to emit with every span and event. Any existing
     * configured attributes will be dropped. Default = none.
     */
    public OtelRumConfig setGlobalAttributes(Attributes attributes) {
        return setGlobalAttributes(() -> attributes);
    }

    public OtelRumConfig setGlobalAttributes(Supplier<Attributes> globalAttributesSupplier) {
        this.globalAttributesSupplier = globalAttributesSupplier;
        return this;
    }

    public boolean hasGlobalAttributes() {
        Attributes attributes = globalAttributesSupplier.get();
        return attributes != null && !attributes.isEmpty();
    }

    public Supplier<Attributes> getGlobalAttributesSupplier() {
        return globalAttributesSupplier;
    }

    /**
     * Disables the collection of runtime network attributes. See {@link CurrentNetworkProvider} for
     * more information. Default = true.
     *
     * @return this
     */
    public OtelRumConfig disableNetworkAttributes() {
        includeNetworkAttributes = false;
        return this;
    }

    /** Returns true if runtime network attributes are enabled, false otherwise. */
    public boolean shouldIncludeNetworkAttributes() {
        return includeNetworkAttributes;
    }

    /**
     * Disables the collection of events related to the initialization of the OTel Android SDK
     * itself. Default = true.
     *
     * @return this
     */
    public OtelRumConfig disableSdkInitializationEvents() {
        generateSdkInitializationEvents = false;
        return this;
    }

    /** Returns true if the SDK is configured to generate initialization events, false otherwise. */
    public boolean shouldGenerateSdkInitializationEvents() {
        return generateSdkInitializationEvents;
    }

    /**
     * Call this to disable the collection of screen attributes. See {@link
     * ScreenAttributesSpanProcessor} for more information. Default = true.
     *
     * @return this
     */
    public OtelRumConfig disableScreenAttributes() {
        includeScreenAttributes = false;
        return this;
    }

    /** Return true if the SDK should be configured to report screen attributes. */
    public boolean shouldIncludeScreenAttributes() {
        return includeScreenAttributes;
    }

    public DiskBufferingConfiguration getDiskBufferingConfiguration() {
        return diskBufferingConfiguration;
    }

    /**
     * Sets the parameters for caching signals in disk in order to export them later.
     *
     * @return this
     */
    public OtelRumConfig setDiskBufferingConfiguration(
            DiskBufferingConfiguration diskBufferingConfiguration) {
        this.diskBufferingConfiguration = diskBufferingConfiguration;
        return this;
    }

    /**
     * Sets the configuration so that network change monitoring, which is enabled by default, will
     * not be started.
     */
    public OtelRumConfig disableNetworkChangeMonitoring() {
        networkChangeMonitoringEnabled = false;
        return this;
    }

    /** Returns true if network change monitoring is enabled (default = true). */
    public boolean isNetworkChangeMonitoringEnabled() {
        return networkChangeMonitoringEnabled;
    }

    /** Returns true if ANR (application not responding) detection is enabled (default = true). */
    public boolean isAnrDetectionEnabled() {
        return anrDetectionEnabled;
    }

    /**
     * Call this method to disable ANR (application not responding) detection.
     *
     * @return this
     */
    public OtelRumConfig disableAnrDetection() {
        anrDetectionEnabled = false;
        return this;
    }

    /** Returns true if the slow rendering detection instrumentation is enabled. */
    public boolean isSlowRenderingDetectionEnabled() {
        return slowRenderingDetectionEnabled;
    }

    /**
     * Call this method to disable the slow rendering detection instrumentation.
     *
     * @return this
     */
    public OtelRumConfig disableSlowRenderingDetection() {
        slowRenderingDetectionEnabled = false;
        return this;
    }

    /** Returns the Duration at which slow renders are polled. Default = 1s. */
    public Duration getSlowRenderingDetectionPollInterval() {
        return slowRenderingDetectionPollInterval;
    }

    /**
     * Call this to configure the duration for polling for slow renders.
     *
     * @return this
     */
    public OtelRumConfig setSlowRenderingDetectionPollInterval(Duration duration) {
        slowRenderingDetectionPollInterval = duration;
        return this;
    }
}
