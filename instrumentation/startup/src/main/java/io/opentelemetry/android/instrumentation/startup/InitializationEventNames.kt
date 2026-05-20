/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

internal object InitializationEventNames {
    const val INIT_EVENT_STARTED: String = "rum.sdk.init.started"
    const val INIT_EVENT_NET_PROVIDER: String = "rum.sdk.init.net.provider"
    const val INIT_EVENT_NET_MONITOR: String = "rum.sdk.init.net.monitor"
    const val INIT_EVENT_ANR_MONITOR: String = "rum.sdk.init.anr_monitor"
    const val INIT_EVENT_JANK_MONITOR: String = "rum.sdk.init.jank_monitor"
    const val INIT_EVENT_CRASH_REPORTER: String = "rum.sdk.init.crash.reporter"
    const val INIT_EVENT_SPAN_EXPORTER: String = "rum.sdk.init.span.exporter"
}
