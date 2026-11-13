/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.Manifest.permission.READ_BASIC_PHONE_STATE
import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_TELEPHONY
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NetworkProviderHolderTest {
    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        NetworkProviderHolder.set(null)
    }

    @After
    fun tearDown() {
        NetworkProviderHolder.close()
    }

    @Test
    fun `get returns same provider instance when called multiple times`() {
        val provider1 = NetworkProviderHolder.get(context)
        val provider2 = NetworkProviderHolder.get(context)

        assertThat(provider1).isNotNull
        assertThat(provider1).isSameAs(provider2)
    }

    @Test
    fun `get safely handles null connectivity service without crashing`() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null

        val provider = NetworkProviderHolder.get(context)

        assertThat(provider).isNotNull
        assertThat(provider).isInstanceOf(CurrentNetworkProvider::class.java)
    }

    @Test
    fun `close closes existing provider and resets instance`() {
        val mockProvider: CurrentNetworkProvider = mockk(relaxed = true)
        NetworkProviderHolder.set(mockProvider)

        NetworkProviderHolder.close()

        verify { mockProvider.close() }

        val newProvider = NetworkProviderHolder.get(context)
        assertThat(newProvider).isNotSameAs(mockProvider)
    }

    @Test
    fun `set overrides singleton instance`() {
        val mockProvider: CurrentNetworkProvider = mockk(relaxed = true)
        NetworkProviderHolder.set(mockProvider)

        assertThat(NetworkProviderHolder.get(context)).isSameAs(mockProvider)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `phoneStatePermissionGrantCheckTrigger creates polling trigger for READ_PHONE_STATE on API 33+`() {
        mockkStatic(ContextCompat::class)
        try {
            val packageManager = mockk<PackageManager>()
            every { context.packageName } returns "app"
            every { context.packageManager } returns packageManager
            every { packageManager.hasSystemFeature(FEATURE_TELEPHONY) } returns true
            @Suppress("DEPRECATION")
            every { packageManager.getPackageInfo("app", PackageManager.GET_PERMISSIONS) } returns
                PackageInfo().apply { requestedPermissions = arrayOf(READ_PHONE_STATE) }

            every { ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) } returns PERMISSION_DENIED
            every { ContextCompat.checkSelfPermission(context, READ_BASIC_PHONE_STATE) } returns PERMISSION_DENIED

            val trigger = NetworkProviderHolder.phoneStatePermissionGrantCheckTrigger(context)

            assertThat(trigger).isInstanceOf(PollingPermissionGrantCheckTrigger::class.java)

            every { ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) } returns PERMISSION_GRANTED

            assertThat(trigger.consumeTrigger()).isTrue()
            assertThat(trigger.consumeTrigger()).isFalse()
        } finally {
            unmockkStatic(ContextCompat::class)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `phoneStatePermissionGrantCheckTrigger returns NONE when READ_BASIC_PHONE_STATE granted on API 33+`() {
        mockkStatic(ContextCompat::class)
        try {
            val packageManager = mockk<PackageManager>()
            every { context.packageName } returns "app"
            every { context.packageManager } returns packageManager
            every { packageManager.hasSystemFeature(FEATURE_TELEPHONY) } returns true
            @Suppress("DEPRECATION")
            every { packageManager.getPackageInfo("app", PackageManager.GET_PERMISSIONS) } returns
                PackageInfo().apply { requestedPermissions = arrayOf(READ_BASIC_PHONE_STATE) }

            every { ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) } returns PERMISSION_DENIED
            // READ_BASIC_PHONE_STATE is a normal permission granted at installation on Android 13+
            every { ContextCompat.checkSelfPermission(context, READ_BASIC_PHONE_STATE) } returns PERMISSION_GRANTED

            val trigger = NetworkProviderHolder.phoneStatePermissionGrantCheckTrigger(context)

            assertThat(trigger).isSameAs(PermissionGrantCheckTrigger.NONE)
        } finally {
            unmockkStatic(ContextCompat::class)
        }
    }

    @Test
    fun `phoneStatePermissionGrantCheckTrigger returns NONE when phone state permission is not requested in manifest`() {
        val packageManager = mockk<PackageManager>()
        every { context.packageName } returns "app"
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(FEATURE_TELEPHONY) } returns true
        @Suppress("DEPRECATION")
        every { packageManager.getPackageInfo("app", PackageManager.GET_PERMISSIONS) } returns
            PackageInfo().apply { requestedPermissions = arrayOf("android.permission.INTERNET") }

        val trigger = NetworkProviderHolder.phoneStatePermissionGrantCheckTrigger(context)

        assertThat(trigger).isSameAs(PermissionGrantCheckTrigger.NONE)
    }
}
