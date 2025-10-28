/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.opentelemetry.android.common.internal.utils.threadIdCompat
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.semconv.ExceptionAttributes
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class CrashReportIntegrationTest {
    private lateinit var rule: OpenTelemetryRule
    private lateinit var installationContext: InstallationContext
    private lateinit var instrumentation: CrashReporterInstrumentation

    @Before
    fun setUp() {
        rule = OpenTelemetryRule.create()
        installationContext = fakeInstallationContext(rule.openTelemetry)
        instrumentation = CrashReporterInstrumentation()
    }

    @Test
    fun `test crash reporter instrumentation is installed`() {
        assertEquals("crash", instrumentation.name)
        instrumentation.install(installationContext)

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        assertTrue(handler is CrashReportingExceptionHandler)
    }

    @Test
    fun `test uncaught exception captured as log`() {
        val exc = IllegalStateException("Whoops")
        val log = simulateUncaughtException(exc)

        log.assertCrashCaptured(
            expectedStacktrace = exc.stackTraceToString(),
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.IllegalStateException",
            expectedExcMessage = "Whoops",
        )
    }

    @Test
    fun `test exception with no message`() {
        val exc = IllegalArgumentException()
        val log = simulateUncaughtException(exc)

        log.assertCrashCaptured(
            expectedStacktrace = exc.stackTraceToString(),
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.IllegalArgumentException",
        )
    }

    @Test
    fun `test java lang error`() {
        val exc = OutOfMemoryError()
        val log = simulateUncaughtException(exc)

        log.assertCrashCaptured(
            expectedStacktrace = exc.stackTraceToString(),
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.OutOfMemoryError",
        )
    }

    @Test
    fun `test additional attributes extractors`() {
        instrumentation.addAttributesExtractor(CustomAttributesExtractor(mapOf("key1" to "value1")))
        instrumentation.addAttributesExtractor(CustomAttributesExtractor(mapOf("key2" to "value2")))

        val exc = IllegalArgumentException()
        val log = simulateUncaughtException(exc)

        log.assertCrashCaptured(
            expectedStacktrace = exc.stackTraceToString(),
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.IllegalArgumentException",
        )
        val attributes = rule.logRecords.single().attributes
        val attrs = attributes.asMap().mapKeys { it.key.key }
        assertEquals("value1", attrs["key1"])
        assertEquals("value2", attrs["key2"])
    }

    @Test
    fun `test stackoverflow behavior`() {
        val exc = generateLargeStacktraceException()
        val log = simulateUncaughtException(exc)
        val stacktrace = exc.stackTraceToString()

        // TODO: future: truncate stackframes at some reasonable limit
        log.assertCrashCaptured(
            expectedStacktrace = stacktrace,
            thread = Thread.currentThread(),
            expectedExcType = exc.javaClass.name,
        )
    }

    @Test
    fun `test zero length stacktrace handled correctly`() {
        val exc = ZeroLengthStackTraceException()
        val log = simulateUncaughtException(exc)

        log.assertCrashCaptured(
            expectedStacktrace = "",
            thread = Thread.currentThread(),
            expectedExcType = exc.javaClass.name,
        )
    }

    @Test
    fun `test previous handler invoked if set`() {
        var invoked = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            invoked = true
        }
        val exc = IllegalArgumentException()
        simulateUncaughtException(exc)
        assertTrue(invoked)
    }

    @Test
    fun `test uncaught exception on different thread`() {
        val exc = IllegalArgumentException()
        lateinit var thread: Thread
        val latch = CountDownLatch(1)

        Executors.newSingleThreadExecutor().submit {
            simulateUncaughtException(exc)
            thread = Thread.currentThread()
            latch.countDown()
        }
        latch.await(100, TimeUnit.MILLISECONDS)

        val log = rule.logRecords.single()
        log.assertCrashCaptured(
            expectedStacktrace = exc.stackTraceToString(),
            thread = thread,
            expectedExcType = "java.lang.IllegalArgumentException",
        )
    }

    @Test(expected = OutOfMemoryError::class)
    fun `test crash in crash handler throws and terminates process`() {
        val exc = ThrowingException()
        simulateUncaughtException(exc)
    }

    /**
     * Simulates an uncaught exception on the current thread and returns the log record
     * from [FakeLogRecordExporter]
     */
    private fun simulateUncaughtException(throwable: Throwable): LogRecordData {
        instrumentation.install(fakeInstallationContext(rule.openTelemetry))
        val handler = checkNotNull(Thread.getDefaultUncaughtExceptionHandler())
        val thread = Thread.currentThread()
        handler.uncaughtException(thread, throwable)
        return rule.logRecords.single()
    }

    /**
     * Asserts that a log record was created with the expected crash details.
     */
    private fun LogRecordData.assertCrashCaptured(
        expectedStacktrace: String,
        thread: Thread,
        expectedExcType: String,
        expectedExcMessage: String? = null,
    ) {
        assertEquals("device.crash", eventName)
        assertEquals(Severity.UNDEFINED_SEVERITY_NUMBER, severity)

        val attrs = attributes.asMap().mapKeys { it.key.key }
        assertEquals(expectedStacktrace, attrs[ExceptionAttributes.EXCEPTION_STACKTRACE.key])
        assertEquals(expectedExcType, attrs[ExceptionAttributes.EXCEPTION_TYPE.key])
        assertEquals(expectedExcMessage, attrs[ExceptionAttributes.EXCEPTION_MESSAGE.key])
        assertEquals(thread.threadIdCompat, attrs[ThreadIncubatingAttributes.THREAD_ID.key])
        assertEquals(thread.name, attrs[ThreadIncubatingAttributes.THREAD_NAME.key])
        assertNotNull(attrs["heap.free"])
        assertNotNull(attrs["storage.free"])
    }

    private fun generateLargeStacktraceException(depth: Int = 1000): Throwable {
        if (depth <= 0) {
            return IllegalStateException()
        }
        return generateLargeStacktraceException(depth - 1)
    }

    private class ZeroLengthStackTraceException : RuntimeException() {
        override fun printStackTrace(s: PrintWriter) {
            // no-op
        }
    }

    private class ThrowingException : RuntimeException() {
        override fun printStackTrace(s: PrintWriter): Unit = throw OutOfMemoryError()
    }

    private class CustomAttributesExtractor(
        private val map: Map<String, String>,
    ) : EventAttributesExtractor<CrashDetails> {
        override fun extract(
            parentContext: Context,
            subject: CrashDetails,
        ): Attributes {
            val builder = Attributes.builder()
            map.forEach {
                builder.put(AttributeKey.stringKey(it.key), it.value)
            }
            return builder.build()
        }
    }
}
