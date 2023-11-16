/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

public interface Lifecycle {
    default void start() {}

    default void stop() {}
}
