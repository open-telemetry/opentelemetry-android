/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import io.opentelemetry.context.propagation.TextMapSetter;
import javax.annotation.Nullable;

enum ClientRequestHeaderSetter implements TextMapSetter<RequestWrapper> {
    INSTANCE;

    @Override
    public void set(@Nullable RequestWrapper requestWrapper, String key, String value) {
        if (requestWrapper != null) {
            requestWrapper.getAdditionalHeaders().put(key, value);
        }
    }
}
