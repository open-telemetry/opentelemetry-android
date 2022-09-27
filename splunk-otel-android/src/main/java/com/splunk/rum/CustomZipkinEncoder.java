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

import java.nio.charset.StandardCharsets;
import java.util.List;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.codec.Encoding;
import zipkin2.internal.JsonCodec;
import zipkin2.internal.V2SpanWriter;
import zipkin2.internal.WriteBuffer;

/**
 * We need a custom encoder to correct for the fact that the zipkin Span.Builder lowercases all Span
 * names.
 *
 * <p>We do this by having the {@link RumAttributeAppender} add an additional attribute ({@link
 * RumAttributeAppender#SPLUNK_OPERATION_KEY}) with the span name properly cased, then correcting
 * the span name here at encoding time.
 */
class CustomZipkinEncoder implements BytesEncoder<Span> {

    private final WriteBuffer.Writer<Span> writer = new V2SpanWriter();

    @Override
    public Encoding encoding() {
        return Encoding.JSON;
    }

    @Override
    public int sizeInBytes(Span span) {
        return this.writer.sizeInBytes(span);
    }

    @Override
    public byte[] encode(Span span) {
        String properSpanName = span.tags().get(RumAttributeAppender.SPLUNK_OPERATION_KEY.getKey());

        // note: this can be optimized, if necessary. Let's keep it simple for now.
        byte[] rawBytes = JsonCodec.write(this.writer, span);
        String renamedResult =
                new String(rawBytes, StandardCharsets.UTF_8)
                        .replace(
                                "\"name\":\"" + span.name() + "\"",
                                "\"name\":\"" + properSpanName + "\"");
        return renamedResult.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encodeList(List<Span> spans) {
        // note: this doesn't appear to be called in our current code paths, so let's leave it be.
        return JsonCodec.writeList(this.writer, spans);
    }
}
