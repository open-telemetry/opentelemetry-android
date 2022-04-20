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

import java.util.Objects;
import java.util.Optional;

class CurrentNetwork {
    private final NetworkState state;
    private final String subType;

    CurrentNetwork(NetworkState state, String subType) {
        this.state = state;
        this.subType = subType;
    }

    boolean isOnline() {
        return getState() != NetworkState.NO_NETWORK_AVAILABLE;
    }

    public NetworkState getState() {
        return state;
    }

    public Optional<String> getSubType() {
        return Optional.ofNullable(subType);
    }

    @Override
    public String toString() {
        return "CurrentNetwork{" + "state=" + state + ", subType='" + subType + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentNetwork that = (CurrentNetwork) o;
        return state == that.state && Objects.equals(subType, that.subType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, subType);
    }
}
