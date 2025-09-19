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
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NetworkDetectorTest {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var context: Context
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities
    private lateinit var packageManager: PackageManager
    private lateinit var contextCompatMock: MockedStatic<ContextCompat>

    @Before
    fun setup() {
        connectivityManager = Mockito.mock(ConnectivityManager::class.java)
        telephonyManager = Mockito.mock(TelephonyManager::class.java)
        context = Mockito.mock(Context::class.java)
        network = Mockito.mock(Network::class.java)
        networkCapabilities = Mockito.mock(NetworkCapabilities::class.java)
        packageManager = Mockito.mock(PackageManager::class.java)
        contextCompatMock = Mockito.mockStatic(ContextCompat::class.java)

        Mockito
            .`when`(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)
        Mockito
            .`when`(context.getSystemService(Context.TELEPHONY_SERVICE))
            .thenReturn(telephonyManager)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)

        // Mock telephony feature as available by default
        Mockito
            .`when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            .thenReturn(true)

        // Mock permission granted by default for most tests
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)
    }

    @After
    fun tearDown() {
        contextCompatMock.close()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun noNetwork_modern() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(null)

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertEquals(CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE), currentNetwork)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun unknown_modern() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(null)
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertEquals(CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN), currentNetwork)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun wifi_modern() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(true)
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertEquals(CurrentNetwork(NetworkState.TRANSPORT_WIFI), currentNetwork)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun cellular_modern() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)

        // Setup for Carrier and SubType details
        Mockito
            .`when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            .thenReturn(true)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)
        Mockito.`when`(telephonyManager.simCarrierId).thenReturn(310)
        Mockito
            .`when`<CharSequence?>(telephonyManager.simCarrierIdName)
            .thenReturn("TestCarrier")
        Mockito.`when`(telephonyManager.simCountryIso).thenReturn("us")
        Mockito.`when`(telephonyManager.simOperator).thenReturn("310260")
        Mockito
            .`when`(telephonyManager.dataNetworkType)
            .thenReturn(TelephonyManager.NETWORK_TYPE_LTE)
        val expectedCarrier = Carrier(310, "TestCarrier", "310", "260", "us")

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertEquals(
            CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, expectedCarrier, "LTE"),
            currentNetwork,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun cellular_modern_withoutPermission() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        // Without permission, should still detect cellular but with no subtype
        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
        assertNull(currentNetwork.subType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun cellular_modern_noTelephonyFeature() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)
        Mockito
            .`when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            .thenReturn(false)
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        // Without telephony feature, should still detect cellular but with limited info
        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun other_modern() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()
        assertEquals(CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN), currentNetwork)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    @Suppress("deprecation")
    fun subtype_preApi24_withPermission() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)
        Mockito
            .`when`(telephonyManager.networkType)
            .thenReturn(TelephonyManager.NETWORK_TYPE_UMTS)

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
        assertEquals("UMTS", currentNetwork.subType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun subtype_securityException_postApi24() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)
        Mockito
            .`when`(telephonyManager.dataNetworkType)
            .thenThrow(SecurityException("Permission denied"))

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
        assertNull(currentNetwork.subType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    @Suppress("deprecation")
    fun subtype_securityException_preApi24() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)
        Mockito
            .`when`(telephonyManager.networkType)
            .thenThrow(SecurityException("Permission denied"))

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
        assertNull(currentNetwork.subType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun cellular_api33_withBasicPhoneStatePermission() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)

        // Deny READ_PHONE_STATE but grant READ_BASIC_PHONE_STATE
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

        Mockito.`when`(telephonyManager.simCarrierId).thenReturn(123)
        Mockito
            .`when`<CharSequence?>(telephonyManager.simCarrierIdName)
            .thenReturn("API33Carrier")
        Mockito.`when`(telephonyManager.simCountryIso).thenReturn("us")
        Mockito.`when`(telephonyManager.simOperator).thenReturn("31026")
        Mockito
            .`when`(telephonyManager.dataNetworkType)
            .thenReturn(TelephonyManager.NETWORK_TYPE_NR)

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
        assertEquals("NR", currentNetwork.subType)
        assertEquals("API33Carrier", currentNetwork.carrierName)
        assertEquals("310", currentNetwork.carrierCountryCode)
        assertEquals("26", currentNetwork.carrierNetworkCode)
        assertEquals("us", currentNetwork.carrierIsoCountryCode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun cellular_api33_withoutAnyPermission() {
        Mockito.`when`<Network?>(connectivityManager.activeNetwork).thenReturn(network)
        Mockito
            .`when`<NetworkCapabilities?>(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        Mockito
            .`when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)

        // Deny both permissions
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)
        contextCompatMock
            .`when`<Any?> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_BASIC_PHONE_STATE,
                )
            }.thenReturn(PackageManager.PERMISSION_DENIED)

        Mockito.`when`(telephonyManager.simCarrierId).thenReturn(123)
        Mockito
            .`when`<CharSequence?>(telephonyManager.simCarrierIdName)
            .thenReturn("API33Carrier")
        Mockito.`when`(telephonyManager.simCountryIso).thenReturn("us")
        Mockito.`when`(telephonyManager.simOperator).thenReturn("31026")

        val networkDetector = NetworkDetector.create(context)
        val currentNetwork = networkDetector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.state)
        assertNull(currentNetwork.subType)
        // Carrier should still be available for basic info (non-permission protected methods)
        assertEquals("310", currentNetwork.carrierCountryCode)
        assertEquals("26", currentNetwork.carrierNetworkCode)
        assertEquals("us", currentNetwork.carrierIsoCountryCode)
    }
}
