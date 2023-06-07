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

package io.opentelemetry.rum.internal.instrumentation.network;

import android.os.Build;
import androidx.annotation.Nullable;
import java.util.Objects;

/** A value class representing the current network that the device is connected to. */
public final class CurrentNetwork {

    @Nullable private final Carrier carrier;
    private final NetworkState state;
    @Nullable private final String subType;

    private CurrentNetwork(Builder builder) {
        this.carrier = builder.carrier;
        this.state = builder.state;
        this.subType = builder.subType;
    }

    /** Returns {@code true} if the device has internet connection; {@code false} otherwise. */
    public boolean isOnline() {
        return getState() != NetworkState.NO_NETWORK_AVAILABLE;
    }

    NetworkState getState() {
        return state;
    }

    @Nullable
    String getSubType() {
        return subType;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    String getCarrierCountryCode() {
        return haveCarrier() ? carrier.getMobileCountryCode() : null;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    String getCarrierIsoCountryCode() {
        return haveCarrier() ? carrier.getIsoCountryCode() : null;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    String getCarrierNetworkCode() {
        return haveCarrier() ? carrier.getMobileNetworkCode() : null;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    String getCarrierName() {
        return haveCarrier() ? carrier.getName() : null;
    }

    private boolean haveCarrier() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) && (carrier != null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentNetwork that = (CurrentNetwork) o;
        return Objects.equals(carrier, that.carrier)
                && state == that.state
                && Objects.equals(subType, that.subType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrier, state, subType);
    }

    @Override
    public String toString() {
        return "CurrentNetwork{"
                + "carrier="
                + carrier
                + ", state="
                + state
                + ", subType='"
                + subType
                + '\''
                + '}';
    }

    static Builder builder(NetworkState state) {
        return new Builder(state);
    }

    static class Builder {
        @Nullable private Carrier carrier;
        private final NetworkState state;
        @Nullable private String subType;

        private Builder(NetworkState state) {
            this.state = state;
        }

        Builder carrier(@Nullable Carrier carrier) {
            this.carrier = carrier;
            return this;
        }

        Builder subType(@Nullable String subType) {
            this.subType = subType;
            return this;
        }

        CurrentNetwork build() {
            return new CurrentNetwork(this);
        }
    }
}
