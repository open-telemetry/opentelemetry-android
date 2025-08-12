/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class NetworkUtilsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
    }

    @Test
    fun testHasReadPhoneStatePermissionGranted() {
        val contextCompatMock = Mockito.mockStatic(ContextCompat::class.java)
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)
        val result = NetworkUtils.hasReadPhoneStatePermission(context)
        Assertions.assertThat(result).isTrue()
        contextCompatMock.close()
    }

    @Test
    fun testHasReadPhoneStatePermissionDenied() {
        val contextCompatMock = Mockito.mockStatic(ContextCompat::class.java)
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)
        val result = NetworkUtils.hasReadPhoneStatePermission(context)
        Assertions.assertThat(result).isFalse()
        contextCompatMock.close()
    }

    @Test
    fun testGetNetworkTypeNameKnownTypes() {
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_1xRTT))
            .isEqualTo("1xRTT")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_CDMA))
            .isEqualTo("CDMA")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EDGE))
            .isEqualTo("EDGE")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EHRPD))
            .isEqualTo("EHRPD")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EVDO_0))
            .isEqualTo("EVDO_0")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EVDO_A))
            .isEqualTo("EVDO_A")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EVDO_B))
            .isEqualTo("EVDO_B")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_GPRS))
            .isEqualTo("GPRS")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_GSM))
            .isEqualTo("GSM")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSDPA))
            .isEqualTo("HSDPA")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSPA))
            .isEqualTo("HSPA")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSPAP))
            .isEqualTo("HSPAP")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSUPA))
            .isEqualTo("HSUPA")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_IWLAN))
            .isEqualTo("IWLAN")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_LTE))
            .isEqualTo("LTE")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_NR))
            .isEqualTo("NR")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_TD_SCDMA))
            .isEqualTo("TD_SCDMA")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_UMTS))
            .isEqualTo("UMTS")
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_UNKNOWN))
            .isEqualTo("UNKNOWN")
    }

    @Test
    @Suppress("DEPRECATION")
    fun testGetNetworkTypeNameDeprecatedIden() {
        Assertions
            .assertThat(NetworkUtils.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_IDEN))
            .isEqualTo("IDEN")
    }

    @Test
    fun testGetNetworkTypeNameUnknownType() {
        val unknownType = 999
        Assertions.assertThat(NetworkUtils.getNetworkTypeName(unknownType)).isEqualTo("UNKNOWN")
    }

    @Test
    fun testIsValidStringWithValidStrings() {
        Assertions.assertThat(NetworkUtils.isValidString("test")).isTrue()
        Assertions.assertThat(NetworkUtils.isValidString("a")).isTrue()
        Assertions.assertThat(NetworkUtils.isValidString("123")).isTrue()
        Assertions.assertThat(NetworkUtils.isValidString("valid string")).isTrue()
    }

    @Test
    fun testIsValidStringWithInvalidStrings() {
        Assertions.assertThat(NetworkUtils.isValidString(null)).isFalse()
        Assertions.assertThat(NetworkUtils.isValidString("")).isFalse()
    }

    @Test
    fun testIsValidStringWithCharSequence() {
        val sb = StringBuilder("test")
        Assertions.assertThat(NetworkUtils.isValidString(sb)).isTrue()

        val emptySb = StringBuilder()
        Assertions.assertThat(NetworkUtils.isValidString(emptySb)).isFalse()
    }
}
