/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public interface NetworkChangeListener {

    void onNetworkChange(CurrentNetwork currentNetwork);
}
