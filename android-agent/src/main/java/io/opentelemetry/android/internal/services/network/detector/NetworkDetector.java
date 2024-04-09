/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import io.opentelemetry.android.internal.services.network.CarrierFinder;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;

public interface NetworkDetector {
    CurrentNetwork detectCurrentNetwork();

    static NetworkDetector create(Context context) {
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
