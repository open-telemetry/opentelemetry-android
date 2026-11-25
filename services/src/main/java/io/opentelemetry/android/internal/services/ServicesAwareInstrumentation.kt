/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

/**
 * This interface is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface ServicesAwareInstrumentation {
    /** Supplies shared SDK services after instrumentation configuration and before installation. */
    fun configureServices(services: ServicesFactory)
}
