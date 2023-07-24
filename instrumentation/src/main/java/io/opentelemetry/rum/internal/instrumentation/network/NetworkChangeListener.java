/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation.network;

interface NetworkChangeListener {

    void onNetworkChange(CurrentNetwork currentNetwork);
}
