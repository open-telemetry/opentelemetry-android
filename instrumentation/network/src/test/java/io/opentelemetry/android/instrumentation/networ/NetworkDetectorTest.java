/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.networ;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class NetworkDetectorTest {

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void quiznos() {
        Context context = ApplicationProvider.getApplicationContext();

        NetworkDetector networkDetector = NetworkDetector.create(context);
        assertTrue(networkDetector instanceof PostApi28NetworkDetector);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void lollipop() {
        Context context = ApplicationProvider.getApplicationContext();

        NetworkDetector networkDetector = NetworkDetector.create(context);
        assertTrue(networkDetector instanceof SimpleNetworkDetector);
    }
}
