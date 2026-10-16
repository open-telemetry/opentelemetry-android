/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalApi::class)

package io.opentelemetry.android.test.common

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.OpenTelemetry
import io.opentelemetry.kotlin.toOtelKotlinApi
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule

/**
 * The opentelemetry-kotlin [OpenTelemetry] API for this rule, mirroring
 * `OpenTelemetryRum.openTelemetryKotlin` in production code.
 */
@ExperimentalApi
val OpenTelemetryRule.openTelemetryKotlin: OpenTelemetry
    get() = openTelemetry.toOtelKotlinApi()
