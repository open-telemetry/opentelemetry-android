/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import java.io.Closeable

interface Service : Closeable {
    override fun close() {}
}
