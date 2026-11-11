/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.minversions.consumer

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.android.openTelemetryKotlin
import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.OpenTelemetry

/**
 * Compiles against the published API the way the README tells consumers to, so that the fixture
 * fails on an API or Kotlin metadata incompatibility and not only on dependency resolution.
 */
@OptIn(ExperimentalApi::class)
class ConsumerApplication : Application() {
    private lateinit var rum: OpenTelemetryRum
    private lateinit var otelKotlin: OpenTelemetry

    override fun onCreate() {
        super.onCreate()
        rum = OpenTelemetryRumInitializer.initialize(this) { disableMetrics() }
        otelKotlin = rum.openTelemetryKotlin
    }
}
