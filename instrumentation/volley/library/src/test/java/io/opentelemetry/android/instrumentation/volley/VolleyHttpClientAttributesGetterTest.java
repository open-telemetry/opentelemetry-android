/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VolleyHttpClientAttributesGetterTest {

    @Test
    public void testRequestHeader() throws Exception {

        RequestWrapper requestWrapper = mock(RequestWrapper.class);
        Request<?> request = mock(Request.class);

        doReturn(request).when(requestWrapper).getRequest();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("foo", "bar");
        requestHeaders.put("aye", "b");
        requestHeaders.put("Foo", "baz");
        requestHeaders.put("FOO", "beep");
        requestHeaders.put("Content-Type", "application/json");

        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Aye", "beeee");
        when(requestWrapper.getAdditionalHeaders()).thenReturn(additionalHeaders);

        doReturn(requestHeaders).when(request).getHeaders();

        VolleyHttpClientAttributesGetter testClass = VolleyHttpClientAttributesGetter.INSTANCE;

        List<String> values = testClass.getHttpRequestHeader(requestWrapper, "content-type");
        assertThat(values).containsExactly("application/json");

        List<String> fooValues = testClass.getHttpRequestHeader(requestWrapper, "FOO");
        assertThat(fooValues).containsExactly("bar", "baz", "beep");
        List<String> ayeValues = testClass.getHttpRequestHeader(requestWrapper, "aYe");
        assertThat(ayeValues).contains("b", "beeee");
    }

    @Test
    public void testResponseHeader() {

        RequestWrapper request = mock(RequestWrapper.class);
        HttpResponse response = mock(HttpResponse.class);

        List<Header> responseHeaders =
                Arrays.asList(
                        new Header("Foo", "bar"),
                        new Header("Foo", "baz"),
                        new Header("Content-Type", "application/json"));
        when(response.getHeaders()).thenReturn(responseHeaders);

        VolleyHttpClientAttributesGetter testClass = VolleyHttpClientAttributesGetter.INSTANCE;

        List<String> values = testClass.getHttpResponseHeader(request, response, "content-type");
        assertThat(values).containsExactly("application/json");

        List<String> fooValues = testClass.getHttpResponseHeader(request, response, "FOO");
        assertThat(fooValues).containsExactly("bar", "baz");
    }

    @Test
    public void testNullResponse() {
        VolleyHttpClientAttributesGetter testClass = VolleyHttpClientAttributesGetter.INSTANCE;
        List<String> values = testClass.getHttpResponseHeader(null, null, "content-type");
        assertThat(values).isEmpty();
    }
}
