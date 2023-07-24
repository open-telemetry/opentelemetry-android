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
import android.telephony.TelephonyManager;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.P)
final class Carrier {

    private final int id;
    private final @Nullable String name;
    private final @Nullable String mobileCountryCode; // 3 digits
    private final @Nullable String mobileNetworkCode; // 2 or 3 digits
    private final @Nullable String isoCountryCode;

    static Builder builder() {
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

    static class Builder {
        private int id = TelephonyManager.UNKNOWN_CARRIER_ID;
        private @Nullable String name = null;
        private @Nullable String mobileCountryCode = null;
        private @Nullable String mobileNetworkCode = null;
        private @Nullable String isoCountryCode = null;

        Carrier build() {
            return new Carrier(this);
        }

        Builder id(int id) {
            this.id = id;
            return this;
        }

        Builder name(String name) {
            this.name = name;
            return this;
        }

        Builder mobileCountryCode(String countryCode) {
            this.mobileCountryCode = countryCode;
            return this;
        }

        Builder mobileNetworkCode(String networkCode) {
            this.mobileNetworkCode = networkCode;
            return this;
        }

        Builder isoCountryCode(String isoCountryCode) {
            this.isoCountryCode = isoCountryCode;
            return this;
        }
    }
}
