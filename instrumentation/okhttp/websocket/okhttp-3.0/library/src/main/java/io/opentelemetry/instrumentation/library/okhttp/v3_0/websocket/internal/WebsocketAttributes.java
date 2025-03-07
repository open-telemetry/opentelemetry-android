/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal;

import io.opentelemetry.api.common.AttributeKey;

public class WebsocketAttributes {
    private WebsocketAttributes() {}

    public static AttributeKey<Long> MESSAGE_SIZE = AttributeKey.longKey("websocket.message.size");
    public static AttributeKey<String> MESSAGE_TYPE =
            AttributeKey.stringKey("websocket.message.type");
}
