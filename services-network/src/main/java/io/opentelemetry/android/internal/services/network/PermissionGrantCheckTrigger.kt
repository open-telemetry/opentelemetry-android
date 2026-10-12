/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

/**
 * Reports when a runtime permission that was missing at startup gets granted. Android does not
 * notify an app of its own permission grants, so implementations have to look for them.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal fun interface PermissionGrantCheckTrigger {
    /** Returns true at most once: on the first call after the permission was granted. */
    fun consumeTrigger(): Boolean

    companion object {
        /** For a permission that cannot be granted after startup, so there is never a grant. */
        val NONE = PermissionGrantCheckTrigger { false }
    }
}
