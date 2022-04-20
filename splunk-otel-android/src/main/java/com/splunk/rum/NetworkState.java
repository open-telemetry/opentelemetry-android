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

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

enum NetworkState {
    NO_NETWORK_AVAILABLE(SemanticAttributes.NetHostConnectionTypeValues.UNAVAILABLE),
    TRANSPORT_CELLULAR(SemanticAttributes.NetHostConnectionTypeValues.CELL),
    TRANSPORT_WIFI(SemanticAttributes.NetHostConnectionTypeValues.WIFI),
    TRANSPORT_UNKNOWN(SemanticAttributes.NetHostConnectionTypeValues.UNKNOWN),
    // this one doesn't seem to have an otel value at this point.
    TRANSPORT_VPN("vpn");

    private final String humanName;

    NetworkState(String humanName) {
        this.humanName = humanName;
    }

    public String getHumanName() {
        return humanName;
    }
}
