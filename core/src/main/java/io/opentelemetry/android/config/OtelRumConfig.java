/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.android.CpuAttributesSpanAppender;
import io.opentelemetry.android.ScreenAttributesSpanProcessor;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Configuration object for OpenTelemetry Android. The configuration items in this class will be
 * used in the OpenTelemetryRumBuilder to wire up and enable/disable various mobile instrumentation
 * components.
 */
public class OtelRumConfig {

    @Nullable private Supplier<Attributes> globalAttributesSupplier = null;
    private boolean includeNetworkAttributes = true;
    private boolean generateSdkInitializationEvents = true;
    private boolean includeScreenAttributes = true;
    private boolean discoverInstrumentations = true;
    private boolean includeCpuAttributes = true;
    private DiskBufferingConfig diskBufferingConfig = DiskBufferingConfig.create();
    private final List<String> suppressedInstrumentations = new ArrayList<>();

    /**
     * Configures the set of global attributes to emit with every span and event. Any existing
     * configured attributes will be dropped. Default = none.
     */
    public OtelRumConfig setGlobalAttributes(@Nullable Attributes attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return this;
        }
        return setGlobalAttributes(() -> attributes);
    }

    public OtelRumConfig setGlobalAttributes(Supplier<Attributes> globalAttributesSupplier) {
        this.globalAttributesSupplier = globalAttributesSupplier;
        return this;
    }

    public boolean hasGlobalAttributes() {
        return globalAttributesSupplier != null;
    }

    @NonNull
    public Supplier<Attributes> getGlobalAttributesSupplier() {
        return globalAttributesSupplier == null ? Attributes::empty : globalAttributesSupplier;
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

    /**
     * Disables the cpu attributes for spans. See {@link CpuAttributesSpanAppender} for more
     * information. Default = true.
     */
    public OtelRumConfig disableCpuAttributes() {
        includeCpuAttributes = false;
        return this;
    }

    /** Returns true if runtime network attributes are enabled, false otherwise. */
    public boolean shouldIncludeNetworkAttributes() {
        return includeNetworkAttributes;
    }

    /** Returns true if cpu attributes are enabled, false otherwise */
    public boolean shouldIncludeCpuAttributes() {
        return includeCpuAttributes;
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

    public DiskBufferingConfig getDiskBufferingConfig() {
        return diskBufferingConfig;
    }

    /**
     * Return {@link Boolean#TRUE} if the RUM initialization should look for instrumentations in the
     * classpath and apply them automatically.
     */
    public boolean shouldDiscoverInstrumentations() {
        return discoverInstrumentations;
    }

    /**
     * Call this to disable the automatic search for instrumentations in the classpath.
     *
     * @return this
     */
    public OtelRumConfig disableInstrumentationDiscovery() {
        discoverInstrumentations = false;
        return this;
    }

    /**
     * Sets the parameters for caching signals in disk in order to export them later.
     *
     * @return this
     */
    public OtelRumConfig setDiskBufferingConfig(DiskBufferingConfig diskBufferingConfig) {
        this.diskBufferingConfig = diskBufferingConfig;
        return this;
    }

    /**
     * Adds an instrumentation name to the list of suppressed instrumentations. Instrumentations
     * that have been suppressed will not be installed at startup.
     */
    public OtelRumConfig suppressInstrumentation(String instrumentationName) {
        suppressedInstrumentations.add(instrumentationName);
        return this;
    }

    /** Returns false when the given instrumentation has been suppressed. True otherwise. */
    public boolean isSuppressed(String instrumentationName) {
        return suppressedInstrumentations.contains(instrumentationName);
    }
}
