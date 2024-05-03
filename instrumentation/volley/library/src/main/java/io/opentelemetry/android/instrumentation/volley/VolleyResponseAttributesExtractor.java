/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.*;

import androidx.annotation.Nullable;
import com.android.volley.Header;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

class VolleyResponseAttributesExtractor
        implements AttributesExtractor<RequestWrapper, HttpResponse> {

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, RequestWrapper requestWrapper) {}

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            RequestWrapper requestWrapper,
            @Nullable HttpResponse response,
            @Nullable Throwable error) {
        if (response != null) {
            onResponse(attributes, response);
        }
    }

    private void onResponse(AttributesBuilder attributes, HttpResponse response) {
        for (Header header : response.getHeaders()) {
            if (header.getName().equals("Content-Length")) {
                String contentLength = header.getValue();
                if (contentLength != null) {
                    attributes.put(HTTP_RESPONSE_BODY_SIZE, Long.parseLong(contentLength));
                }
            }
        }
    }
}
