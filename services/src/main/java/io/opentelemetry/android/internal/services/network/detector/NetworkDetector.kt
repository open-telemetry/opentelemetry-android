/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.network.CarrierFinder;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public interface NetworkDetector {
    CurrentNetwork detectCurrentNetwork();

    static NetworkDetector create(Context context) {
        // TODO: Use ServiceManager to get the ConnectivityManager (not yet managed)
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            CarrierFinder carrierFinder = new CarrierFinder(telephonyManager);
            return new PostApi28NetworkDetector(
                    connectivityManager, telephonyManager, carrierFinder, context);
        }
        return new SimpleNetworkDetector(connectivityManager);
    }
}
