/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import io.opentelemetry.android.internal.services.network.data.CurrentNetwork

interface NetworkChangeListener {
    fun onNetworkChange(currentNetwork: CurrentNetwork)
}
