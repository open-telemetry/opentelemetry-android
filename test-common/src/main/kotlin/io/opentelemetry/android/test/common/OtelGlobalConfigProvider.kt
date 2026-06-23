/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import org.robolectric.annotation.Config
import org.robolectric.pluginapi.config.GlobalConfigProvider

class OtelGlobalConfigProvider : GlobalConfigProvider {
    override fun get(): Config = Config.Builder().setSdk(Config.NEWEST_SDK).build()
}
