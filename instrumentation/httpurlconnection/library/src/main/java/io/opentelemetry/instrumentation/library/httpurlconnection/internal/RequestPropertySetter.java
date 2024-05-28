/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal;

import io.opentelemetry.context.propagation.TextMapSetter;
import java.net.URLConnection;

public enum RequestPropertySetter implements TextMapSetter<URLConnection> {
    INSTANCE;

    @Override
    public void set(URLConnection carrier, String key, String value) {
        carrier.setRequestProperty(key, value);
    }
}
