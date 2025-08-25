/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.Manifest.permission.READ_BASIC_PHONE_STATE
import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_TELEPHONY
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT
import android.telephony.TelephonyManager.NETWORK_TYPE_CDMA
import android.telephony.TelephonyManager.NETWORK_TYPE_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD
import android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0
import android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A
import android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B
import android.telephony.TelephonyManager.NETWORK_TYPE_GPRS
import android.telephony.TelephonyManager.NETWORK_TYPE_GSM
import android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA
import android.telephony.TelephonyManager.NETWORK_TYPE_HSPA
import android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP
import android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA
import android.telephony.TelephonyManager.NETWORK_TYPE_IDEN
import android.telephony.TelephonyManager.NETWORK_TYPE_IWLAN
import android.telephony.TelephonyManager.NETWORK_TYPE_LTE
import android.telephony.TelephonyManager.NETWORK_TYPE_NR
import android.telephony.TelephonyManager.NETWORK_TYPE_TD_SCDMA
import android.telephony.TelephonyManager.NETWORK_TYPE_UMTS
import android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NetworkUtilsTest {
    private lateinit var context: Context
    private lateinit var contextCompatMock: MockedStatic<ContextCompat>

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
        contextCompatMock = Mockito.mockStatic(ContextCompat::class.java)
    }

    @After
    fun tearDown() {
        contextCompatMock.close()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun testHasPhoneStatePermissionGranted_preApi33() {
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_GRANTED)

        val result = hasPhoneStatePermission(context)
        assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun testHasPhoneStatePermissionDenied_preApi33() {
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_DENIED)

        val result = hasPhoneStatePermission(context)
        assertThat(result).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithBasicPermission_api33() {
        // Deny READ_PHONE_STATE but grant READ_BASIC_PHONE_STATE
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_DENIED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_GRANTED)

        val result = hasPhoneStatePermission(context)
        assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithFullPermission_api33() {
        // Grant READ_PHONE_STATE, deny READ_BASIC_PHONE_STATE
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_GRANTED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_DENIED)

        val result = hasPhoneStatePermission(context)
        assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithBothPermissions_api33() {
        // Grant both permissions
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_GRANTED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_GRANTED)

        val result = hasPhoneStatePermission(context)
        assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithoutAnyPermission_api33() {
        // Deny both permissions
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_DENIED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PERMISSION_DENIED)

        val result = hasPhoneStatePermission(context)
        assertThat(result).isFalse()
    }

    @Test
    fun testHasTelephonyFeatureTrue() {
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito
            .`when`(packageManager.hasSystemFeature(FEATURE_TELEPHONY))
            .thenReturn(true)

        val result = hasTelephonyFeature(context)
        assertThat(result).isTrue()
    }

    @Test
    fun testHasTelephonyFeatureFalse() {
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito
            .`when`(packageManager.hasSystemFeature(FEATURE_TELEPHONY))
            .thenReturn(false)

        val result = hasTelephonyFeature(context)
        assertThat(result).isFalse()
    }

    @Test
    @Suppress("DEPRECATION")
    fun testGetNetworkTypeNameKnownTypes() {
        // If this were junit5 we could have a more sane parameterized test....
        val types =
            mapOf(
                NETWORK_TYPE_1xRTT to "1xRTT",
                NETWORK_TYPE_CDMA to "CDMA",
                NETWORK_TYPE_EDGE to "EDGE",
                NETWORK_TYPE_EHRPD to "EHRPD",
                NETWORK_TYPE_EVDO_0 to "EVDO_0",
                NETWORK_TYPE_EVDO_A to "EVDO_A",
                NETWORK_TYPE_EVDO_B to "EVDO_B",
                NETWORK_TYPE_GPRS to "GPRS",
                NETWORK_TYPE_GSM to "GSM",
                NETWORK_TYPE_HSDPA to "HSDPA",
                NETWORK_TYPE_HSPA to "HSPA",
                NETWORK_TYPE_HSPAP to "HSPAP",
                NETWORK_TYPE_HSUPA to "HSUPA",
                NETWORK_TYPE_IWLAN to "IWLAN",
                NETWORK_TYPE_LTE to "LTE",
                NETWORK_TYPE_NR to "NR",
                NETWORK_TYPE_TD_SCDMA to "TD_SCDMA",
                NETWORK_TYPE_UMTS to "UMTS",
                NETWORK_TYPE_UNKNOWN to "UNKNOWN",
            )
        types.forEach { networkType, expected ->
            assertThat(getNetworkTypeName(networkType)).isEqualTo(expected)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun testGetNetworkTypeNameDeprecatedIden() {
        assertThat(getNetworkTypeName(NETWORK_TYPE_IDEN))
            .isEqualTo("IDEN")
    }

    @Test
    fun testGetNetworkTypeNameUnknownType() {
        val unknownType = 999
        assertThat(getNetworkTypeName(unknownType)).isEqualTo("UNKNOWN")
    }

    @Test
    fun testIsValidStringWithValidStrings() {
        assertThat(isValidString("test")).isTrue()
        assertThat(isValidString("a")).isTrue()
        assertThat(isValidString("123")).isTrue()
        assertThat(isValidString("valid string")).isTrue()
    }

    @Test
    fun testIsValidStringWithInvalidStrings() {
        assertThat(isValidString(null)).isFalse()
        assertThat(isValidString("")).isFalse()
    }

    @Test
    fun testIsValidStringWithCharSequence() {
        val sb = StringBuilder("test")
        assertThat(isValidString(sb)).isTrue()

        val emptySb = StringBuilder()
        assertThat(isValidString(emptySb)).isFalse()
    }
}
