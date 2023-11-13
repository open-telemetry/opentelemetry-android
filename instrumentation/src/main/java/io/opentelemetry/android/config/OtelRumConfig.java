/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config;

import java.util.function.Supplier;

import io.opentelemetry.android.ScreenAttributesSpanProcessor;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.api.common.Attributes;

/**
 * Configuration object for OpenTelemetry Android. The configuration items in this class will be
 * used in the OpenTelemetryRumBuilder to wire up and enable/disable various mobile instrumentation
 * components.
 */
public class OtelRumConfig {

    private Supplier<Attributes> globalAttributesSupplier = Attributes::empty;
    private boolean includeNetworkAttributes = true;
    private boolean generateSdkInitializationEvents = true;
    private boolean includeScreenAttributes = true;
    private PersistenceConfiguration persistenceConfiguration;

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

    boolean hasGlobalAttributes() {
        Attributes attributes = globalAttributesSupplier.get();
        return attributes != null && !attributes.isEmpty();
    }

    Supplier<Attributes> getGlobalAttributesSupplier() {
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

    /**
     * Returns true if runtime network attributes are enabled, false otherwise.
     */
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
        this.generateSdkInitializationEvents = false;
        return this;
    }

    /**
     * Returns true if the SDK is configured to generate initialization events, false otherwise.
     */
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
        this.includeScreenAttributes = false;
        return this;
    }

    /**
     * Return true if the SDK should be configured to report screen attributes.
     */
    public boolean shouldIncludeScreenAttributes() {
        return includeScreenAttributes;
    }

    public PersistenceConfiguration getPersistenceConfiguration() {
        if (persistenceConfiguration == null) {
            return PersistenceConfiguration.builder().build();
        }
        return persistenceConfiguration;
    }

    /**
     * Sets the parameters for caching signals in disk in order to export them later.
     */
    public void setPersistenceConfiguration(PersistenceConfiguration persistenceConfiguration) {
        this.persistenceConfiguration = persistenceConfiguration;
    }
}
