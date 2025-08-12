/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class SubTypeFinderTest {
    private lateinit var context: Context
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var subTypeFinder: SubTypeFinder

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
        telephonyManager = Mockito.mock(TelephonyManager::class.java)
        // Default: permission granted unless a test overrides it
        Mockito
            .`when`(
                context.checkPermission(anyString(), anyInt(), anyInt()),
            ).thenReturn(PackageManager.PERMISSION_GRANTED)
        subTypeFinder = SubTypeFinder(context, telephonyManager)
    }

    @Test
    fun `returns null when permission is denied`() {
        Mockito
            .`when`(
                context.checkPermission(anyString(), anyInt(), anyInt()),
            ).thenReturn(PackageManager.PERMISSION_DENIED)

        val result = subTypeFinder.get()
        assertThat(result).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `returns LTE for post API 24 with permission`() {
        Mockito
            .`when`(telephonyManager.getDataNetworkType())
            .thenReturn(TelephonyManager.NETWORK_TYPE_LTE)

        val result = subTypeFinder.get()
        assertThat(result).isEqualTo("LTE")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    @Suppress("DEPRECATION")
    fun `returns UMTS for pre API 24 with permission`() {
        Mockito
            .`when`(telephonyManager.getNetworkType())
            .thenReturn(TelephonyManager.NETWORK_TYPE_UMTS)

        val result = subTypeFinder.get()
        assertThat(result).isEqualTo("UMTS")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `returns null if SecurityException thrown post API 24`() {
        Mockito
            .`when`(telephonyManager.getDataNetworkType())
            .thenThrow(SecurityException("Permission denied"))

        val result = subTypeFinder.get()
        assertThat(result).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    @Suppress("DEPRECATION")
    fun `returns null if SecurityException thrown pre API 24`() {
        Mockito
            .`when`(telephonyManager.getNetworkType())
            .thenThrow(SecurityException("Permission denied"))

        val result = subTypeFinder.get()
        assertThat(result).isNull()
    }
}
