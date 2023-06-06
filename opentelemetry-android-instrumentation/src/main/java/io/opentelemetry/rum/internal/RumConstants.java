/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal;

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

    public static final AttributeKey<String> PREVIOUS_SESSION_ID_KEY =
            stringKey("rum.session.previous_id");

    public static final String APP_START_SPAN_NAME = "AppStart";

    private RumConstants() {}
}
