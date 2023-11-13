/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

public interface Service extends Lifecycle {

    Type type();

    enum Type {
        APPLICATION_INFO,
        PREFERENCES
    }
}
