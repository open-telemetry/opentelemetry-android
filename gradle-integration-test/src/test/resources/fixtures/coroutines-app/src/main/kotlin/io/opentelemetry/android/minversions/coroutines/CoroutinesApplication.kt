/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.minversions.coroutines

import android.app.Application
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Calls the API that the coroutines agent weaves, so the weaving has something to rewrite.
 */
class CoroutinesApplication : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        OpenTelemetryRumInitializer.initialize(this)
    }

    fun launchWork(): Job = scope.launch { yield() }
}
