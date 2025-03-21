/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData
import io.opentelemetry.sdk.testing.assertj.LogRecordDataAssert
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat

fun LogRecordDataAssert.hasEventName(eventName: String): LogRecordDataAssert {
    isNotNull()
    // TODO: This will be removed after the event bits of logs gets out of incubating.
    assertThat(this.actual()).isInstanceOf(ExtendedLogRecordData::class.java)
    assertThat((this.actual() as ExtendedLogRecordData).eventName).isEqualTo(eventName)
    return this
}
