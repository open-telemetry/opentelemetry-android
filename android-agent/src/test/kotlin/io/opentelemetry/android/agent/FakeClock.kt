/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.sdk.common.Clock

class FakeClock : Clock {
    override fun now(): Long = 0

    override fun nanoTime(): Long = 0
}
