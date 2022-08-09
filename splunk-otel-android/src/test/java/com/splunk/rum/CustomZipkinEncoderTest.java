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
import static org.junit.Assert.assertEquals;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import org.junit.Test;
import zipkin2.Endpoint;
import zipkin2.Span;

public class CustomZipkinEncoderTest {

    @Test
    public void nameReplacement() {
        CustomZipkinEncoder encoder = new CustomZipkinEncoder();
        Span span =
                Span.newBuilder()
                        .name("lowercase")
                        .traceId(TraceId.fromLongs(1, 2))
                        .id(SpanId.fromLong(1))
                        .putTag(RumAttributeAppender.SPLUNK_OPERATION_KEY.getKey(), "UpperCase")
                        .build();
        byte[] bytes = encoder.encode(span);
        // this assertion verifies that we changed the name
        assertEquals(
                "{\"traceId\":\"00000000000000010000000000000002\",\"id\":\"0000000000000001\",\"name\":\"UpperCase\",\"tags\":{\"_splunk_operation\":\"UpperCase\"}}",
                new String(bytes));
        assertEquals(bytes.length, encoder.sizeInBytes(span));
    }

    @Test
    public void removeLocalIp() {
        Span span =
                Span.newBuilder()
                        .name("test")
                        .traceId(TraceId.fromLongs(1, 2))
                        .id(SpanId.fromLong(1))
                        .localEndpoint(
                                Endpoint.newBuilder()
                                        .serviceName("test-app")
                                        .ip("127.0.0.1")
                                        .build())
                        .build();

        CustomZipkinEncoder encoder = new CustomZipkinEncoder();

        byte[] bytes = encoder.encode(span);
        assertThat(new String(bytes))
                .contains("\"localEndpoint\":{\"serviceName\":\"test-app\"}")
                .doesNotContain(
                        "\"localEndpoint\":{\"serviceName\":\"test-app\",\"ipv4\":\"127.0.0.1\"}");
    }
}
