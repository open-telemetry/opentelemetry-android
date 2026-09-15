/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common.http

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HttpUrlFilterTest {
    @Test
    fun instrumentAllAcceptsEverything() {
        val filter = HttpUrlFilter.instrumentAll()

        assertThat(filter.shouldInstrument("https://api.example.com/v1")).isTrue()
        assertThat(filter.shouldInstrument("not a url")).isTrue()
    }

    @Test
    fun instrumentAllIsASingletonSoCallersCanSkipFiltering() {
        assertThat(HttpUrlFilter.instrumentAll()).isSameAs(HttpUrlFilter.instrumentAll())
    }

    @Test
    fun onlyHostsMatchesExactHost() {
        val filter = HttpUrlFilter.onlyHosts("api.example.com")

        assertThat(filter.shouldInstrument("https://api.example.com/v1/items")).isTrue()
        assertThat(filter.shouldInstrument("https://other.example.com/v1/items")).isFalse()
        assertThat(filter.shouldInstrument("https://example.com/v1/items")).isFalse()
    }

    @Test
    fun onlyHostsIgnoresCasePortPathAndQuery() {
        val filter = HttpUrlFilter.onlyHosts("API.Example.COM")

        assertThat(filter.shouldInstrument("https://api.example.com:8443/v1?q=1#frag")).isTrue()
        assertThat(filter.shouldInstrument("http://API.EXAMPLE.COM")).isTrue()
    }

    @Test
    fun onlyHostsAcceptsAnyOfSeveralPatterns() {
        val filter = HttpUrlFilter.onlyHosts("api.example.com", "*.internal.example.com")

        assertThat(filter.shouldInstrument("https://api.example.com/a")).isTrue()
        assertThat(filter.shouldInstrument("https://svc.internal.example.com/a")).isTrue()
        assertThat(filter.shouldInstrument("https://cdn.thirdparty.net/a")).isFalse()
    }

    @Test
    fun wildcardMatchesSubdomainsAtAnyDepthButNotTheApex() {
        val filter = HttpUrlFilter.onlyHosts("*.example.com")

        assertThat(filter.shouldInstrument("https://a.example.com/x")).isTrue()
        assertThat(filter.shouldInstrument("https://a.b.example.com/x")).isTrue()
        assertThat(filter.shouldInstrument("https://example.com/x")).isFalse()
    }

    @Test
    fun wildcardRequiresALabelBoundary() {
        val filter = HttpUrlFilter.onlyHosts("*.example.com")

        assertThat(filter.shouldInstrument("https://notexample.com/x")).isFalse()
        assertThat(filter.shouldInstrument("https://evil-example.com/x")).isFalse()
        assertThat(filter.shouldInstrument("https://example.com.attacker.net/x")).isFalse()
    }

    @Test
    fun exceptHostsIsTheInverseOfOnlyHosts() {
        val filter = HttpUrlFilter.exceptHosts("cdn.thirdparty.net", "*.ads.example.net")

        assertThat(filter.shouldInstrument("https://api.example.com/a")).isTrue()
        assertThat(filter.shouldInstrument("https://cdn.thirdparty.net/a")).isFalse()
        assertThat(filter.shouldInstrument("https://x.ads.example.net/a")).isFalse()
        assertThat(filter.shouldInstrument("https://ads.example.net/a")).isTrue()
    }

    @Test
    fun urlWithNoDeterminableHostFallsBackToEachFiltersNoMatchAnswer() {
        val urls = listOf("", "not a url", "mailto:someone@example.com", "/relative/path")

        for (url in urls) {
            assertThat(HttpUrlFilter.onlyHosts("api.example.com").shouldInstrument(url))
                .describedAs("onlyHosts should not instrument '%s'", url)
                .isFalse()
            assertThat(HttpUrlFilter.exceptHosts("api.example.com").shouldInstrument(url))
                .describedAs("exceptHosts should instrument '%s'", url)
                .isTrue()
        }
    }

    @Test
    fun userInfoIsNotMistakenForTheHost() {
        val filter = HttpUrlFilter.onlyHosts("api.example.com")

        assertThat(filter.shouldInstrument("https://user@api.example.com/v1")).isTrue()
        assertThat(filter.shouldInstrument("https://user:pw@api.example.com:8443/v1")).isTrue()
        assertThat(filter.shouldInstrument("https://api.example.com@attacker.net/v1")).isFalse()
    }

    @Test
    fun ipv6LiteralHostsMatchExactly() {
        val filter = HttpUrlFilter.onlyHosts("[::1]")

        assertThat(filter.shouldInstrument("http://[::1]:8080/v1")).isTrue()
        assertThat(filter.shouldInstrument("http://127.0.0.1:8080/v1")).isFalse()
    }

    @Test
    fun emptyPatternListIsRejected() {
        assertThatThrownBy { HttpUrlFilter.onlyHosts() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("At least one host pattern")
    }

    @Test
    fun malformedPatternsAreRejectedAtConfigurationTime() {
        val rejected =
            mapOf(
                "" to "must not be blank",
                "   " to "must not be blank",
                "exa mple.com" to "whitespace",
                "https://api.example.com" to "must be a host, not a URL",
                "api.example.com/v1" to "must be a host, not a URL",
                "api.example.com?q=1" to "must be a host, not a URL",
                "api.example.com:8443" to "must not include a port",
                "exa*ple.com" to "leading '*.' label",
                "*" to "leading '*.' label",
                "*." to "must name a domain after",
            )

        for ((pattern, expectedMessage) in rejected) {
            assertThatThrownBy { HttpUrlFilter.onlyHosts(pattern) }
                .describedAs("pattern '%s'", pattern)
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining(expectedMessage)
        }
    }

    @Test
    fun toStringNamesTheFilterAndItsPatterns() {
        assertThat(HttpUrlFilter.onlyHosts("*.example.com").toString())
            .isEqualTo("HttpUrlFilter.onlyHosts(*.example.com)")
        assertThat(HttpUrlFilter.instrumentAll().toString())
            .isEqualTo("HttpUrlFilter.instrumentAll()")
    }
}
