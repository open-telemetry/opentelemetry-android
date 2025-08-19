/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
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
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

        val result = hasPhoneStatePermission(context)
        Assertions.assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun testHasPhoneStatePermissionDenied_preApi33() {
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        val result = hasPhoneStatePermission(context)
        Assertions.assertThat(result).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithBasicPermission_api33() {
        // Deny READ_PHONE_STATE but grant READ_BASIC_PHONE_STATE
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

        val result = hasPhoneStatePermission(context)
        Assertions.assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithFullPermission_api33() {
        // Grant READ_PHONE_STATE, deny READ_BASIC_PHONE_STATE
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        val result = hasPhoneStatePermission(context)
        Assertions.assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithBothPermissions_api33() {
        // Grant both permissions
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

        val result = hasPhoneStatePermission(context)
        Assertions.assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPhoneStatePermissionWithoutAnyPermission_api33() {
        // Deny both permissions
        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        contextCompatMock
            .`when`<Int> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        val result = hasPhoneStatePermission(context)
        Assertions.assertThat(result).isFalse()
    }

    @Test
    fun testHasTelephonyFeatureTrue() {
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito
            .`when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            .thenReturn(true)

        val result = hasTelephonyFeature(context)
        Assertions.assertThat(result).isTrue()
    }

    @Test
    fun testHasTelephonyFeatureFalse() {
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito
            .`when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            .thenReturn(false)

        val result = hasTelephonyFeature(context)
        Assertions.assertThat(result).isFalse()
    }

    @Test
    @Suppress("DEPRECATION")
    fun testGetNetworkTypeNameKnownTypes() {
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_1xRTT))
            .isEqualTo("1xRTT")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_CDMA))
            .isEqualTo("CDMA")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EDGE))
            .isEqualTo("EDGE")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EHRPD))
            .isEqualTo("EHRPD")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EVDO_0))
            .isEqualTo("EVDO_0")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EVDO_A))
            .isEqualTo("EVDO_A")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EVDO_B))
            .isEqualTo("EVDO_B")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_GPRS))
            .isEqualTo("GPRS")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_GSM))
            .isEqualTo("GSM")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSDPA))
            .isEqualTo("HSDPA")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSPA))
            .isEqualTo("HSPA")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSPAP))
            .isEqualTo("HSPAP")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_HSUPA))
            .isEqualTo("HSUPA")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_IWLAN))
            .isEqualTo("IWLAN")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_LTE))
            .isEqualTo("LTE")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_NR))
            .isEqualTo("NR")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_TD_SCDMA))
            .isEqualTo("TD_SCDMA")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_UMTS))
            .isEqualTo("UMTS")
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_UNKNOWN))
            .isEqualTo("UNKNOWN")
    }

    @Test
    @Suppress("DEPRECATION")
    fun testGetNetworkTypeNameDeprecatedIden() {
        Assertions
            .assertThat(getNetworkTypeName(TelephonyManager.NETWORK_TYPE_IDEN))
            .isEqualTo("IDEN")
    }

    @Test
    fun testGetNetworkTypeNameUnknownType() {
        val unknownType = 999
        Assertions.assertThat(getNetworkTypeName(unknownType)).isEqualTo("UNKNOWN")
    }

    @Test
    fun testIsValidStringWithValidStrings() {
        Assertions.assertThat(isValidString("test")).isTrue()
        Assertions.assertThat(isValidString("a")).isTrue()
        Assertions.assertThat(isValidString("123")).isTrue()
        Assertions.assertThat(isValidString("valid string")).isTrue()
    }

    @Test
    fun testIsValidStringWithInvalidStrings() {
        Assertions.assertThat(isValidString(null)).isFalse()
        Assertions.assertThat(isValidString("")).isFalse()
    }

    @Test
    fun testIsValidStringWithCharSequence() {
        val sb = StringBuilder("test")
        Assertions.assertThat(isValidString(sb)).isTrue()

        val emptySb = StringBuilder()
        Assertions.assertThat(isValidString(emptySb)).isFalse()
    }
}
