/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.periodic.TaskScheduler
import io.opentelemetry.android.internal.services.storage.CacheStorage
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import java.io.Closeable

interface ServicesFactory : Closeable {
    val cacheStorage: CacheStorage
    val taskScheduler: TaskScheduler
    val currentNetworkProvider: CurrentNetworkProvider
    val appLifecycle: AppLifecycle
    val visibleScreenTracker: VisibleScreenTracker
}
