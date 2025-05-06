/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click.internal

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey

const val APP_SCREEN_CLICK_EVENT_NAME = "app.screen.click"
const val VIEW_CLICK_EVENT_NAME = "event.app.widget.click"
val viewNameAttr: AttributeKey<String> = stringKey("app.widget.name")

val xCoordinateAttr: AttributeKey<Long> = longKey("app.screen.coordinate.x")
val yCoordinateAttr: AttributeKey<Long> = longKey("app.screen.coordinate.y")
val viewIdAttr: AttributeKey<Long> = longKey("app.widget.id")
