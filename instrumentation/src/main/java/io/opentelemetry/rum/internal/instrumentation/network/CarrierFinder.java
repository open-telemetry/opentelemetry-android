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
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.P)
class CarrierFinder {

    private final TelephonyManager telephonyManager;

    CarrierFinder(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    Carrier get() {
        Carrier.Builder builder = Carrier.builder();
        int id = telephonyManager.getSimCarrierId();
        builder.id(id);
        CharSequence name = telephonyManager.getSimCarrierIdName();
        if (validString(name)) {
            builder.name(name.toString());
        }
        String simOperator = telephonyManager.getSimOperator();
        if (validString(simOperator) && simOperator.length() >= 5) {
            String countryCode = simOperator.substring(0, 3);
            String networkCode = simOperator.substring(3);
            builder.mobileCountryCode(countryCode).mobileNetworkCode(networkCode);
        }
        String isoCountryCode = telephonyManager.getSimCountryIso();
        if (validString(isoCountryCode)) {
            builder.isoCountryCode(isoCountryCode);
        }
        return builder.build();
    }

    private boolean validString(CharSequence str) {
        return !(str == null || str.length() == 0);
    }
}
