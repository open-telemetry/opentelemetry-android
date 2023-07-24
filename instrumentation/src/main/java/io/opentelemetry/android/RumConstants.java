/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

public class RumConstants {

    public static final String OTEL_RUM_LOG_TAG = "OpenTelemetryRum";

    public static final AttributeKey<String> SESSION_ID_KEY = stringKey("rum.session.id");

    public static final AttributeKey<String> LAST_SCREEN_NAME_KEY =
            AttributeKey.stringKey("last.screen.name");
    public static final AttributeKey<String> SCREEN_NAME_KEY =
            AttributeKey.stringKey("screen.name");
    public static final AttributeKey<String> START_TYPE_KEY = stringKey("start.type");

    public static final AttributeKey<String> RUM_SDK_VERSION = stringKey("rum.sdk.version");

    public static final AttributeKey<Long> STORAGE_SPACE_FREE_KEY = longKey("storage.free");
    public static final AttributeKey<Long> HEAP_FREE_KEY = longKey("heap.free");
    public static final AttributeKey<Double> BATTERY_PERCENT_KEY = doubleKey("battery.percent");

    public static final AttributeKey<String> PREVIOUS_SESSION_ID_KEY =
            stringKey("rum.session.previous_id");

    public static final String APP_START_SPAN_NAME = "AppStart";

    private RumConstants() {}
}
