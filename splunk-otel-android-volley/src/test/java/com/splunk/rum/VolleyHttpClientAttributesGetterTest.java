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

        List<String> values = testClass.requestHeader(requestWrapper, "content-type");
        assertThat(values).containsExactly("application/json");

        List<String> fooValues = testClass.requestHeader(requestWrapper, "FOO");
        assertThat(fooValues).containsExactly("bar", "baz", "beep");
        List<String> ayeValues = testClass.requestHeader(requestWrapper, "aYe");
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

        List<String> values = testClass.responseHeader(request, response, "content-type");
        assertThat(values).containsExactly("application/json");

        List<String> fooValues = testClass.responseHeader(request, response, "FOO");
        assertThat(fooValues).containsExactly("bar", "baz");
    }

    @Test
    public void testNullResponse() {
        VolleyHttpClientAttributesGetter testClass = VolleyHttpClientAttributesGetter.INSTANCE;
        List<String> values = testClass.responseHeader(null, null, "content-type");
        assertThat(values).isEmpty();
    }
}
