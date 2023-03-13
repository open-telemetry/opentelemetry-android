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

package com.splunk.rum;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.rum.internal.SpanFilterBuilder;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * This class hold {@link AttributeKey}s for standard RUM-related attributes that are not in the
 * OpenTelemetry {@link io.opentelemetry.semconv.trace.attributes.SemanticAttributes} definitions.
 */
public final class StandardAttributes {
    /**
     * The version of your app. Useful for adding to global attributes.
     *
     * @see SplunkRumBuilder#setGlobalAttributes(Attributes)
     */
    public static final AttributeKey<String> APP_VERSION = AttributeKey.stringKey("app.version");

    /**
     * The build type of your app (typically one of debug or release). Useful for adding to global
     * attributes.
     *
     * @see SplunkRumBuilder#setGlobalAttributes(Attributes)
     */
    public static final AttributeKey<String> APP_BUILD_TYPE =
            AttributeKey.stringKey("app.build.type");

    /**
     * Full HTTP client request URL in the form {@code scheme://host[:port]/path?query[#fragment]}.
     * Useful for span data filtering with the {@link SpanFilterBuilder}.
     *
     * @see SemanticAttributes#HTTP_URL
     */
    public static final AttributeKey<String> HTTP_URL = SemanticAttributes.HTTP_URL;

    private StandardAttributes() {}
}
