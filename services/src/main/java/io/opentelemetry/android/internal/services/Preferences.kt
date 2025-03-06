/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Utility to store and retrieve apps' preferences.
 *
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
class Preferences internal constructor(
    private val preferences: SharedPreferences,
) {
    fun store(
        key: String,
        value: Int,
    ) {
        preferences.edit {
            putInt(key, value)
        }
    }

    fun retrieveInt(
        key: String,
        defaultValue: Int,
    ): Int = preferences.getInt(key, defaultValue)
}
