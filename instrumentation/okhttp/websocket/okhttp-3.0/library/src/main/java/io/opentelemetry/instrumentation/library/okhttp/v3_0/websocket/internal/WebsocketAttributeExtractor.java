/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import okhttp3.Request;
import okhttp3.WebSocket;

class WebsocketAttributeExtractor {
    private WebsocketAttributeExtractor() {}

    static Attributes extractAttributes(WebSocket socket) {
        AttributesBuilder builder = Attributes.builder();
        Request request = socket.request();
        builder.put(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket");

        builder.put(HttpAttributes.HTTP_REQUEST_METHOD, request.method());
        builder.put(UrlAttributes.URL_FULL, request.url().toString());
        builder.put(NetworkAttributes.NETWORK_PEER_PORT, request.url().port());

        return builder.build();
    }
}
