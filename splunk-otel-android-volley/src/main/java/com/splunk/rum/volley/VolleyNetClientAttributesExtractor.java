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

package com.splunk.rum.volley;

import com.android.volley.toolbox.HttpResponse;

import androidx.annotation.Nullable;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

final class VolleyNetClientAttributesExtractor extends NetClientAttributesExtractor<RequestWrapper, HttpResponse> {

    @Override
    public String transport(RequestWrapper requestWrapper, @Nullable HttpResponse httpResponse) {
        return SemanticAttributes.NetTransportValues.IP_TCP;
    }

    @Override
    public String peerName(RequestWrapper requestWrapper, @Nullable HttpResponse httpResponse) {
        return requestWrapper.getUrl().getHost();
    }

    @Override
    public Integer peerPort(RequestWrapper requestWrapper, @Nullable HttpResponse httpResponse) {
        return requestWrapper.getUrl().getPort();
    }

    @Override
    public String peerIp(RequestWrapper requestWrapper, @Nullable HttpResponse httpResponse) {
        return null;
    }
}
