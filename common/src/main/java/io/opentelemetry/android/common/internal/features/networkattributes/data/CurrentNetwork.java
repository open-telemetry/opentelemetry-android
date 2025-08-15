/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes.data;

import androidx.annotation.Nullable;
import java.util.Objects;

/**
 * A value class representing the current network that the device is connected to.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
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

    public NetworkState getState() {
        return state;
    }

    @Nullable
    public String getSubType() {
        return subType;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    public String getCarrierCountryCode() {
        return (carrier != null) ? carrier.getMobileCountryCode() : null;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    public String getCarrierIsoCountryCode() {
        return (carrier != null) ? carrier.getIsoCountryCode() : null;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    public String getCarrierNetworkCode() {
        return (carrier != null) ? carrier.getMobileNetworkCode() : null;
    }

    @SuppressWarnings("NullAway")
    @Nullable
    public String getCarrierName() {
        return (carrier != null) ? carrier.getName() : null;
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

    public static Builder builder(NetworkState state) {
        return new Builder(state);
    }

    public static class Builder {
        @Nullable private Carrier carrier;
        private final NetworkState state;
        @Nullable private String subType;

        private Builder(NetworkState state) {
            this.state = state;
        }

        public Builder carrier(@Nullable Carrier carrier) {
            this.carrier = carrier;
            return this;
        }

        public Builder subType(@Nullable String subType) {
            if ((subType != null) && subType.isEmpty()) {
                return subType(null);
            }
            this.subType = subType;
            return this;
        }

        public CurrentNetwork build() {
            return new CurrentNetwork(this);
        }
    }
}
