/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class VolleyResponseAttributesExtractorTest {

    @Test
    public void extractsContentLength() {
        List<Header> responseHeaders =
                Collections.singletonList(new Header("Content-Length", "90210"));
        RequestWrapper fakeRequest =
                new RequestWrapper(mock(Request.class), Collections.emptyMap());
        HttpResponse response = new HttpResponse(200, responseHeaders, "hello".getBytes());

        VolleyResponseAttributesExtractor attributesExtractor =
                new VolleyResponseAttributesExtractor();
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, null, fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, null, fakeRequest, response, null);
        Attributes attributes = attributesBuilder.build();
        assertThat(attributes.get(HTTP_RESPONSE_BODY_SIZE)).isEqualTo(90210L);
    }
}
