/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

@Deprecated
interface SessionChangeObserver {

    /** Gets called every time a new sessionId is generated. */
    void onChange(String oldSessionId, String newSessionId);
}
