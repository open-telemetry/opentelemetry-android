/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

internal interface TestApi : ApiExtension {
    override val type: Class<out ApiExtension> get() = TestApi::class.java
}

internal class TestApiProvider : TestApi

internal interface OtherTestApi : ApiExtension {
    override val type: Class<out ApiExtension> get() = OtherTestApi::class.java
}

internal class OtherTestApiProvider : OtherTestApi
