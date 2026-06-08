/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config

import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig.Companion.create
import io.opentelemetry.api.common.Attributes
import java.util.function.Supplier

/**
 * Configuration object for OpenTelemetry Android. The configuration items in this class will be
 * used in the OpenTelemetryRumBuilder to wire up and enable/disable various mobile instrumentation
 * components. Property reads are not threadsafe.
 */
class OtelRumConfig {
    private var globalAttributesSupplierImpl: Supplier<Attributes>? = null
    private var includeNetworkAttributes = true
    private var generateSdkInitializationEvents = true
    private var includeScreenAttributes = true
    private var discoverInstrumentations = true
    private val suppressedInstrumentations: MutableList<String> = mutableListOf()
    private var diskBufferingConfigImpl: DiskBufferingConfig = create()

    /**
     * Whether the tracing API is enabled. Toggle with using [setTracingEnabled]
     */
    var tracingEnabled = true
        private set

    /**
     * Whether the logging API is enabled. Toggle with using [setLoggingEnabled]
     */
    var loggingEnabled = true
        private set

    /**
     * Whether the metrics API is enabled. Toggle with using [setMetricsEnabled]
     */
    var metricsEnabled = true
        private set

    /**
     * Configures the set of global attributes to emit with every span and event. Any existing
     * configured attributes will be dropped. Default = none.
     */
    fun setGlobalAttributes(attributes: Attributes): OtelRumConfig {
        if (attributes.isEmpty) {
            return this
        }
        return setGlobalAttributes { attributes }
    }

    fun setGlobalAttributes(globalAttributesSupplier: Supplier<Attributes>?): OtelRumConfig {
        this.globalAttributesSupplierImpl = globalAttributesSupplier
        return this
    }

    fun hasGlobalAttributes(): Boolean = globalAttributesSupplierImpl != null

    fun getGlobalAttributesSupplier(): Supplier<Attributes> = globalAttributesSupplierImpl ?: Supplier { Attributes.empty() }

    /**
     * Disables the collection of runtime network attributes. See [CurrentNetworkProvider] for
     * more information. Default = true.
     *
     * @return this
     */
    fun disableNetworkAttributes(): OtelRumConfig {
        includeNetworkAttributes = false
        return this
    }

    /** Returns true if runtime network attributes are enabled, false otherwise.  */
    fun shouldIncludeNetworkAttributes(): Boolean = includeNetworkAttributes

    /**
     * Disables the collection of events related to the initialization of the OTel Android SDK
     * itself. Default = true.
     *
     * @return this
     */
    fun disableSdkInitializationEvents(): OtelRumConfig {
        generateSdkInitializationEvents = false
        return this
    }

    /** Returns true if the SDK is configured to generate initialization events, false otherwise.  */
    fun shouldGenerateSdkInitializationEvents(): Boolean = generateSdkInitializationEvents

    /**
     * Call this to disable the collection of screen attributes. See [ ] for more information. Default = true.
     *
     * @return this
     */
    fun disableScreenAttributes(): OtelRumConfig {
        includeScreenAttributes = false
        return this
    }

    /** Return true if the SDK should be configured to report screen attributes.  */
    fun shouldIncludeScreenAttributes(): Boolean = includeScreenAttributes

    /**
     * Return true if the RUM initialization should look for instrumentations in the
     * classpath and apply them automatically.
     */
    fun shouldDiscoverInstrumentations(): Boolean = discoverInstrumentations

    /**
     * Call this to disable the automatic search for instrumentations in the classpath.
     *
     * @return this
     */
    fun disableInstrumentationDiscovery(): OtelRumConfig {
        discoverInstrumentations = false
        return this
    }

    /**
     * Adds an instrumentation name to the list of suppressed instrumentations. Instrumentations
     * that have been suppressed will not be installed at startup.
     */
    fun suppressInstrumentation(instrumentationName: String): OtelRumConfig {
        suppressedInstrumentations.add(instrumentationName)
        return this
    }

    /**
     * Removes an instrumentation name from the list of suppressed instrumentations.
     * Instrumentations that have been suppressed will not be installed at startup.
     */
    fun allowInstrumentation(instrumentationName: String): OtelRumConfig {
        suppressedInstrumentations.remove(instrumentationName)
        return this
    }

    /** Returns false when the given instrumentation has been suppressed. True otherwise.  */
    fun isSuppressed(instrumentationName: String): Boolean = suppressedInstrumentations.contains(instrumentationName)

    /**
     * Enables or disables the tracing API for the SDK. When disabled, the SDK will provide no-op implementations to instrumentation
     * that use this API.
     */
    fun setTracingEnabled(enabled: Boolean): OtelRumConfig {
        tracingEnabled = enabled
        return this
    }

    /**
     * Enables or disables the logging API for the SDK. When disabled, the SDK will provide no-op implementations to instrumentation
     * that use this API.
     */
    fun setLoggingEnabled(enabled: Boolean): OtelRumConfig {
        loggingEnabled = enabled
        return this
    }

    /**
     * Enables or disables the metrics API for the SDK. When disabled, the SDK will provide no-op implementations to instrumentation
     * that use this API.
     */
    fun setMetricsEnabled(enabled: Boolean): OtelRumConfig {
        metricsEnabled = enabled
        return this
    }

    fun getDiskBufferingConfig(): DiskBufferingConfig = diskBufferingConfigImpl

    /**
     * Sets the parameters for caching signals in disk in order to export them later.
     *
     * @return this
     */
    fun setDiskBufferingConfig(diskBufferingConfig: DiskBufferingConfig): OtelRumConfig {
        this.diskBufferingConfigImpl = diskBufferingConfig
        return this
    }
}
