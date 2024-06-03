/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;

public interface NetworkChangeListener {

    void onNetworkChange(CurrentNetwork currentNetwork);
}
