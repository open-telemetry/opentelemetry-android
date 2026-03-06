/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import android.content.Context
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.Clock

/**
 * Parameters that are useful for installing instrumentation.
 */
interface InstallationContext {

    /**
     * A reference to the application context.
     */
    val context: Context

    /**
     * The [OpenTelemetry] instance used by opentelemetry-android.
     */
    val openTelemetry: OpenTelemetry

    /**
     * The [SessionProvider] that provides details of the current session.
     */
    val sessionProvider: SessionProvider

    /**
     * The [Clock] instance used by opentelemetry-android.
     */
    val clock: Clock

    /**
     * A reference to the application, if available.
     */
    val application: Application?
}
