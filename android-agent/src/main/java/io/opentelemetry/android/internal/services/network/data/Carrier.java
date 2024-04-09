/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.data;

import android.os.Build;
import android.telephony.TelephonyManager;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.P)
public final class Carrier {

    private final int id;
    private final @Nullable String name;
    private final @Nullable String mobileCountryCode; // 3 digits
    private final @Nullable String mobileNetworkCode; // 2 or 3 digits
    private final @Nullable String isoCountryCode;

    public static Builder builder() {
        return new Builder();
    }

    Carrier(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.mobileCountryCode = builder.mobileCountryCode;
        this.mobileNetworkCode = builder.mobileNetworkCode;
        this.isoCountryCode = builder.isoCountryCode;
    }

    int getId() {
        return id;
    }

    @Nullable
    String getName() {
        return name;
    }

    @Nullable
    String getMobileCountryCode() {
        return mobileCountryCode;
    }

    @Nullable
    String getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    @Nullable
    String getIsoCountryCode() {
        return isoCountryCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Carrier carrier = (Carrier) o;
        return id == carrier.id
                && Objects.equals(name, carrier.name)
                && Objects.equals(mobileCountryCode, carrier.mobileCountryCode)
                && Objects.equals(mobileNetworkCode, carrier.mobileNetworkCode)
                && Objects.equals(isoCountryCode, carrier.isoCountryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, mobileCountryCode, mobileNetworkCode, isoCountryCode);
    }

    @Override
    public String toString() {
        return "Carrier{"
                + "id="
                + id
                + ", name='"
                + name
                + '\''
                + ", mobileCountryCode='"
                + mobileCountryCode
                + '\''
                + ", mobileNetworkCode='"
                + mobileNetworkCode
                + '\''
                + ", isoCountryCode='"
                + isoCountryCode
                + '\''
                + '}';
    }

    public static class Builder {
        private int id = TelephonyManager.UNKNOWN_CARRIER_ID;
        private @Nullable String name = null;
        private @Nullable String mobileCountryCode = null;
        private @Nullable String mobileNetworkCode = null;
        private @Nullable String isoCountryCode = null;

        public Carrier build() {
            return new Carrier(this);
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder mobileCountryCode(String countryCode) {
            this.mobileCountryCode = countryCode;
            return this;
        }

        public Builder mobileNetworkCode(String networkCode) {
            this.mobileNetworkCode = networkCode;
            return this;
        }

        public Builder isoCountryCode(String isoCountryCode) {
            this.isoCountryCode = isoCountryCode;
            return this;
        }
    }
}
