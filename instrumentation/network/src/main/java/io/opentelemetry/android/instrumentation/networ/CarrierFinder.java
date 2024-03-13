/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.networ;

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
