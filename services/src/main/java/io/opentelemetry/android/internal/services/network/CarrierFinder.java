/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import static io.opentelemetry.android.internal.services.network.NetworkUtilsKt.hasPhoneStatePermission;
import static io.opentelemetry.android.internal.services.network.NetworkUtilsKt.hasTelephonyFeature;
import static io.opentelemetry.android.internal.services.network.NetworkUtilsKt.isValidString;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public class CarrierFinder {

    private final TelephonyManager telephonyManager;
    private final Context context;

    public CarrierFinder(@NonNull Context context, @NonNull TelephonyManager telephonyManager) {
        this.context = context;
        this.telephonyManager = telephonyManager;
    }

    @Nullable
    public Carrier get() {
        if (!hasTelephonyFeature(context)) {
            Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Cannot determine carrier details: telephony feature missing.");
            return null;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (hasPhoneStatePermission(context)) {
                    return getCarrierPostApi28();
                } else {
                    Log.w(
                            RumConstants.OTEL_RUM_LOG_TAG,
                            "Missing read phone state permission, using legacy carrier methods.");
                    return getCarrierPreApi28();
                }
            } else {
                return getCarrierPreApi28();
            }
        } catch (SecurityException e) {
            Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "SecurityException when accessing carrier info",
                    e);
        }

        return null;
    }

    /** Extracts carrier information using modern APIs (Post API 28). */
    @RequiresApi(Build.VERSION_CODES.P)
    private Carrier getCarrierPostApi28() {
        int id = telephonyManager.getSimCarrierId();

        String name = null;
        CharSequence carrierName = telephonyManager.getSimCarrierIdName();
        if (isValidString(carrierName)) {
            name = carrierName.toString();
        }

        String[] mccMncIso = getMccMncIso();
        return new Carrier(id, name, mccMncIso[0], mccMncIso[1], mccMncIso[2]);
    }

    /** Extracts carrier information using legacy APIs (Pre API 28). */
    private Carrier getCarrierPreApi28() {
        String name = null;
        String carrierName = telephonyManager.getSimOperatorName();
        if (isValidString(carrierName)) {
            name = carrierName;
        } else {
            carrierName = telephonyManager.getNetworkOperatorName();
            if (isValidString(carrierName)) {
                name = carrierName;
            }
        }

        String[] mccMncIso = getMccMncIso();
        return new Carrier(-1, name, mccMncIso[0], mccMncIso[1], mccMncIso[2]);
    }

    /**
     * Extracts MCC, MNC, and ISO country code from TelephonyManager.
     *
     * @return String array: [mcc, mnc, iso]
     */
    private String[] getMccMncIso() {
        String mcc = null, mnc = null, iso = null;
        String simOperator = telephonyManager.getSimOperator();
        if (isValidString(simOperator) && simOperator.length() >= 5) {
            mcc = simOperator.substring(0, 3);
            mnc = simOperator.substring(3);
        }
        String isoCountryCode = telephonyManager.getSimCountryIso();
        if (isValidString(isoCountryCode)) {
            iso = isoCountryCode;
        }
        return new String[] {mcc, mnc, iso};
    }
}
