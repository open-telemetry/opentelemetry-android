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
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class CarrierFinderTest {
    private lateinit var context: Context
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var packageManager: PackageManager
    private lateinit var carrierFinder: CarrierFinder

    @Before
    fun setUp() {
        context = mockk()
        telephonyManager = mockk()
        packageManager = mockk()
        mockkStatic(ContextCompat::class)
        every { context.packageManager } returns packageManager

        // Mock permission granted by default unless overridden in a test
        every {
            ContextCompat.checkSelfPermission(
                context,
                READ_PHONE_STATE,
            )
        } returns PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every {
                ContextCompat.checkSelfPermission(
                    context,
                    READ_BASIC_PHONE_STATE,
                )
            } returns PERMISSION_GRANTED
        }

        carrierFinder = CarrierFinder(context, telephonyManager)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun testPostApi28WithPermission() {
        setupTelephonyCapability(true)

        every { telephonyManager.simCarrierId } returns 123
        every { telephonyManager.simCarrierIdName } returns "TestCarrier"
        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder.get()

        assertThat(carrier!!.id).isEqualTo(123)
        assertThat(carrier.name).isEqualTo("TestCarrier")
        assertThat(carrier.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun testPostApi28WithoutPermission() {
        setupTelephonyCapability(true)

        every { ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) } returns PERMISSION_DENIED

        every { telephonyManager.simOperatorName } returns "testCarrier"
        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder!!.get()

        assertThat(carrier!!.id).isEqualTo(-1)
        assertThat(carrier.name).isEqualTo("testCarrier")
        assertThat(carrier.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun testPreApi28WithValidName() {
        setupTelephonyCapability(true)

        every { telephonyManager.getSimOperatorName() } returns ""
        every { telephonyManager.getNetworkOperatorName() } returns "LegacyCarrier"
        every { telephonyManager.getSimOperator() } returns "31026"
        every { telephonyManager.getSimCountryIso() } returns "nl"

        val carrier = carrierFinder!!.get()

        assertThat(carrier!!.id).isEqualTo(-1)
        assertThat(carrier.name).isEqualTo("LegacyCarrier")
        assertThat(carrier.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun testPreApi28WithInvalidName() {
        setupTelephonyCapability(true)

        every { telephonyManager.simOperatorName } returns ""
        every { telephonyManager.networkOperatorName } returns ""
        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder!!.get()

        assertThat(carrier!!.id).isEqualTo(-1)
        assertThat(carrier.name).isNull()
        assertThat(carrier.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testApi33WithBasicPhoneStatePermission() {
        setupTelephonyCapability(true)

        // Deny READ_PHONE_STATE but grant READ_BASIC_PHONE_STATE
        every { ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) } returns PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, READ_BASIC_PHONE_STATE) } returns PERMISSION_GRANTED

        every { telephonyManager.simCarrierId } returns 456
        every { telephonyManager.simCarrierIdName } returns "API33Carrier"
        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder.get()

        assertThat(carrier!!.id).isEqualTo(456)
        assertThat(carrier.name).isEqualTo("API33Carrier")
        assertThat(carrier.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testApi33WithoutAnyPermission() {
        setupTelephonyCapability(true)

        // Deny both permissions
        every { ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) } returns PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, READ_BASIC_PHONE_STATE) } returns PERMISSION_DENIED

        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.simOperatorName } returns ""
        every { telephonyManager.networkOperatorName } returns ""
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder.get()

        assertThat(carrier!!.id).isEqualTo(-1)
        assertThat(carrier.name).isNull()
        assertThat(carrier.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun testNoTelephonyCapability() {
        setupTelephonyCapability(false)

        val carrier = carrierFinder!!.get()

        assertThat<Carrier?>(carrier).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun testSecurityExceptionOnCarrierAccess() {
        setupTelephonyCapability(true)

        every { telephonyManager.simCarrierId } throws SecurityException("Permission denied")

        val carrier = carrierFinder.get()

        assertThat<Carrier?>(carrier).isNull()
    }

    @Test
    fun testShortSimOperator() {
        setupTelephonyCapability(true)

        every { telephonyManager.simOperator } returns "123" // Too short
        every { telephonyManager.simOperatorName } returns "bmobile"
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder!!.get()

        assertThat(carrier!!.mobileCountryCode).isNull()
        assertThat(carrier.mobileNetworkCode).isNull()
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    fun testInvalidSimOperator() {
        setupTelephonyCapability(true)

        every { telephonyManager.simOperator } returns ""
        every { telephonyManager.simOperatorName } returns "bmobile"
        every { telephonyManager.simCountryIso } returns "nl"

        val carrier = carrierFinder.get()

        assertThat(carrier!!.mobileCountryCode).isNull()
        assertThat(carrier.mobileNetworkCode).isNull()
        assertThat(carrier.isoCountryCode).isEqualTo("nl")
    }

    @Test
    fun testInvalidIsoCountryCode() {
        setupTelephonyCapability(true)

        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.simOperatorName } returns "bmobile"
        every { telephonyManager.simCountryIso } returns ""

        val carrier = carrierFinder!!.get()

        assertThat(carrier!!.mobileCountryCode).isEqualTo("310")
        assertThat(carrier.mobileNetworkCode).isEqualTo("26")
        assertThat(carrier.isoCountryCode).isNull()
    }

    fun setupTelephonyCapability(hasCapability: Boolean) {
        every { packageManager.hasSystemFeature(FEATURE_TELEPHONY) } returns hasCapability
    }
}
