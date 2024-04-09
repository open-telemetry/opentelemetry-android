/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.networkattrs

interface NetworkChangeListener {
    fun onNetworkChange(currentNetwork: CurrentNetwork)
}
