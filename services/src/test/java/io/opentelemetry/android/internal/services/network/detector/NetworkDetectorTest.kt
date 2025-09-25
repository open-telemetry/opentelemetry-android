/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NetworkDetectorTest {
    @MockK
    private lateinit var connectivityManager: ConnectivityManager

    @MockK
    private lateinit var telephonyManager: TelephonyManager

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var network: Network

    @MockK
    private lateinit var networkCapabilities: NetworkCapabilities

    @MockK
    private lateinit var packageManager: PackageManager

    @MockK
    private lateinit var contextCompatMock: ContextCompat

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(ContextCompat::class)
        contextCompatMock = mockk<ContextCompat>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { context.getSystemService(Context.TELEPHONY_SERVICE) } returns telephonyManager
        every { context.packageManager } returns packageManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(any()) } returns false // default
        every { telephonyManager.simOperatorName } returns "JibroCom" // default

        // Mock telephony feature as available by default
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true

        // Mock permission granted by default for most tests
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_GRANTED
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun noNetwork_modern() {
        every { connectivityManager.activeNetwork } returns null

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertThat(currentNetwork).isEqualTo(CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun unknown_modern() {
        every { connectivityManager.getNetworkCapabilities(network) } returns null
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertThat(currentNetwork).isEqualTo(CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun wifi_modern() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertThat(currentNetwork).isEqualTo(CurrentNetwork(NetworkState.TRANSPORT_WIFI))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun cellular_modern() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

        // Setup for Carrier and SubType details
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_GRANTED
        every { telephonyManager.simCarrierId } returns 310
        every { telephonyManager.simCarrierIdName } returns "TestCarrier"
        every { telephonyManager.simCountryIso } returns "us"
        every { telephonyManager.simOperator } returns "310260"
        every { telephonyManager.dataNetworkType } returns TelephonyManager.NETWORK_TYPE_LTE
        val expectedCarrier = Carrier(310, "TestCarrier", "310", "260", "us")

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertThat(currentNetwork).isEqualTo(CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, expectedCarrier, "LTE"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun cellular_modern_withoutPermission() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_DENIED
        every { telephonyManager.simOperator } returns "trev"
        every { telephonyManager.simCountryIso } returns "91"

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        // Without permission, should still detect cellular but with no subtype
        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
        assertThat(currentNetwork.subType).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun cellular_modern_noTelephonyFeature() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns false

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        // Without telephony feature, should still detect cellular but with limited info
        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun other_modern() {
        every { networkCapabilities.hasTransport(any()) } returns false
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertThat(currentNetwork).isEqualTo(CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    @Suppress("deprecation")
    fun subtype_preApi24_withPermission() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_GRANTED
        every { telephonyManager.networkType } returns TelephonyManager.NETWORK_TYPE_UMTS
        every { telephonyManager.simOperator } returns "jeb"
        every { telephonyManager.simCountryIso } returns "1"

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
        assertThat(currentNetwork.subType).isEqualTo("UMTS")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun subtype_securityException_postApi24() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_GRANTED
        every { telephonyManager.dataNetworkType } throws SecurityException("Permission denied")
        every { telephonyManager.simOperator } throws SecurityException("Permission denied")

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
        assertThat(currentNetwork.subType).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    @Suppress("deprecation")
    fun subtype_securityException_preApi24() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_GRANTED
        every { telephonyManager.networkType } throws SecurityException("Permission denied")
        every { telephonyManager.simOperator } returns "gah"
        every { telephonyManager.simCountryIso } returns "81"

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
        assertThat(currentNetwork.subType).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun cellular_api33_withBasicPhoneStatePermission() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        // Deny READ_PHONE_STATE but grant READ_BASIC_PHONE_STATE
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_BASIC_PHONE_STATE) } returns
            PackageManager.PERMISSION_GRANTED

        every { telephonyManager.simCarrierId } returns 123
        every { telephonyManager.simCarrierIdName } returns "API33Carrier"
        every { telephonyManager.simCountryIso } returns "us"
        every { telephonyManager.simOperator } returns "31026"
        every { telephonyManager.dataNetworkType } returns TelephonyManager.NETWORK_TYPE_NR

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
        assertThat(currentNetwork.subType).isEqualTo("NR")
        assertThat(currentNetwork.carrierName).isEqualTo("API33Carrier")
        assertThat(currentNetwork.carrierCountryCode).isEqualTo("310")
        assertThat(currentNetwork.carrierNetworkCode).isEqualTo("26")
        assertThat(currentNetwork.carrierIsoCountryCode).isEqualTo("us")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun cellular_api33_withoutAnyPermission() {
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

        // Deny both permissions
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_BASIC_PHONE_STATE) } returns
            PackageManager.PERMISSION_DENIED

        every { telephonyManager.simCarrierId } returns 123
        every { telephonyManager.simCarrierIdName } returns "API33Carrier"
        every { telephonyManager.simCountryIso } returns "us"
        every { telephonyManager.simOperator } returns "31026"

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertThat(currentNetwork.state).isEqualTo(NetworkState.TRANSPORT_CELLULAR)
        assertThat(currentNetwork.subType).isNull()
        // Carrier should still be available for basic info (non-permission protected methods)
        assertThat(currentNetwork.carrierCountryCode).isEqualTo("310")
        assertThat(currentNetwork.carrierNetworkCode).isEqualTo("26")
        assertThat(currentNetwork.carrierIsoCountryCode).isEqualTo("us")
    }
}
