/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common;

import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

public class RumConstants {

    public static final String OTEL_RUM_LOG_TAG = "OpenTelemetryRum";

    public static final AttributeKey<String> LAST_SCREEN_NAME_KEY =
            AttributeKey.stringKey("last.screen.name");
    public static final AttributeKey<String> SCREEN_NAME_KEY =
            AttributeKey.stringKey("screen.name");
    public static final AttributeKey<String> START_TYPE_KEY = stringKey("start.type");

    public static final AttributeKey<String> RUM_SDK_VERSION = stringKey("rum.sdk.version");

    public static final AttributeKey<Long> STORAGE_SPACE_FREE_KEY = longKey("storage.free");
    public static final AttributeKey<Long> HEAP_FREE_KEY = longKey("heap.free");
    public static final AttributeKey<Double> BATTERY_PERCENT_KEY = doubleKey("battery.percent");

    public static final String APP_START_SPAN_NAME = "AppStart";

    public static final class Events {
        public static final String INIT_EVENT_STARTED = "rum.sdk.init.started";
        public static final String INIT_EVENT_CONFIG = "rum.sdk.init.config";
        public static final String INIT_EVENT_NET_PROVIDER = "rum.sdk.init.net.provider";
        public static final String INIT_EVENT_NET_MONITOR = "rum.sdk.init.net.monitor";
        public static final String INIT_EVENT_ANR_MONITOR = "rum.sdk.init.anr_monitor";
        public static final String INIT_EVENT_JANK_MONITOR = "rum.sdk.init.jank_monitor";
        public static final String INIT_EVENT_CRASH_REPORTER = "rum.sdk.init.crash.reporter";
        public static final String INIT_EVENT_SPAN_EXPORTER = "rum.sdk.init.span.exporter";

        // TODO: Use the semconv when available
        public static final String EVENT_SESSION_START = "session.start";
        public static final String EVENT_SESSION_END = "session.end";

        private Events() {}
    }

    private RumConstants() {}
}
