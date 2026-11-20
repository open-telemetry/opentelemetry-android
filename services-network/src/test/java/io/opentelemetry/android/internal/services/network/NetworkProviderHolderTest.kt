/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
}
