/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.utils

import android.os.Build
import androidx.annotation.RequiresApi

val Thread.threadIdCompat: Long
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            threadIdApi36
        } else {
            @Suppress("DEPRECATION")
            id
        }

@get:RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
internal val Thread.threadIdApi36: Long
    get() = threadId()
