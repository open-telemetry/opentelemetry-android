/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import android.os.Build;
import android.telephony.TelephonyManager;
import androidx.annotation.RequiresApi;
import io.opentelemetry.android.internal.services.network.data.Carrier;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
@RequiresApi(api = Build.VERSION_CODES.P)
public class CarrierFinder {

    private final TelephonyManager telephonyManager;

    public CarrierFinder(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    public Carrier get() {
        int id = telephonyManager.getSimCarrierId();
        String name = null;
        String mobileCountryCode = null;
        String mobileNetworkCode = null;
        String isoCountryCode = null;
        CharSequence simCarrierIdName = telephonyManager.getSimCarrierIdName();
        if (validString(simCarrierIdName)) {
            name = simCarrierIdName.toString();
        }
        String simOperator = telephonyManager.getSimOperator();
        if (validString(simOperator) && simOperator.length() >= 5) {
            mobileCountryCode = simOperator.substring(0, 3);
            mobileNetworkCode = simOperator.substring(3);
        }
        String providedIsoCountryCode = telephonyManager.getSimCountryIso();
        if (validString(providedIsoCountryCode)) {
            isoCountryCode = providedIsoCountryCode;
        }
        return new Carrier(id, name, mobileCountryCode, mobileNetworkCode, isoCountryCode);
    }

    private boolean validString(CharSequence str) {
        return !(str == null || str.length() == 0);
    }
}
