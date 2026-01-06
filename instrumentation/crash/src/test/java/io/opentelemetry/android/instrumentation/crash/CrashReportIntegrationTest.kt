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
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.sdk.trace.data.SpanData
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

    @Test
    fun `test crash emits span when mode is SPANS_ONLY`() {
        instrumentation.setCrashReportingMode(CrashReportingMode.SPANS_ONLY)
        val exc = IllegalStateException("Crash!")
        triggerUncaughtException(exc)

        // Verify span was emitted
        val spans = rule.spans
        assertEquals(1, spans.size)
        val span = spans.single()
        span.assertCrashSpanCaptured(
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.IllegalStateException",
            expectedExcMessage = "Crash!",
        )

        // Verify NO log was emitted
        assertTrue(rule.logRecords.isEmpty())
    }

    @Test
    fun `test crash emits both log and span when mode is LOGS_AND_SPANS`() {
        instrumentation.setCrashReportingMode(CrashReportingMode.LOGS_AND_SPANS)
        val exc = IllegalStateException("Crash!")
        triggerUncaughtException(exc)

        // Verify log was emitted
        assertEquals(1, rule.logRecords.size)
        val log = rule.logRecords.single()
        log.assertCrashCaptured(
            expectedStacktrace = exc.stackTraceToString(),
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.IllegalStateException",
            expectedExcMessage = "Crash!",
        )

        // Verify span was emitted
        assertEquals(1, rule.spans.size)
        val span = rule.spans.single()
        span.assertCrashSpanCaptured(
            thread = Thread.currentThread(),
            expectedExcType = "java.lang.IllegalStateException",
            expectedExcMessage = "Crash!",
        )
    }

    @Test
    fun `test default mode emits only logs`() {
        // Don't set mode - use default (LOGS_ONLY)
        val exc = IllegalStateException("Crash!")
        triggerUncaughtException(exc)

        // Verify log was emitted
        assertEquals(1, rule.logRecords.size)

        // Verify NO span was emitted
        assertTrue(rule.spans.isEmpty())
    }

    @Test
    fun `test span has exception event with correct attributes`() {
        instrumentation.setCrashReportingMode(CrashReportingMode.SPANS_ONLY)
        val exc = IllegalStateException("Test exception message")
        triggerUncaughtException(exc)

        val span = rule.spans.single()

        // Verify exception event on span
        val exceptionEvent = span.events.find { it.name == "exception" }
        assertNotNull("Expected exception event on span", exceptionEvent)

        // Verify exception event attributes
        val eventAttrs = exceptionEvent!!.attributes.asMap().mapKeys { it.key.key }
        assertEquals("java.lang.IllegalStateException", eventAttrs[ExceptionAttributes.EXCEPTION_TYPE.key])
        assertEquals("Test exception message", eventAttrs[ExceptionAttributes.EXCEPTION_MESSAGE.key])
        assertNotNull(eventAttrs[ExceptionAttributes.EXCEPTION_STACKTRACE.key])
    }

    /**
     * Triggers an uncaught exception without assuming a log is emitted.
     * Use this for tests that verify different CrashReportingMode settings.
     */
    private fun triggerUncaughtException(throwable: Throwable) {
        instrumentation.install(fakeInstallationContext(rule.openTelemetry))
        val handler = checkNotNull(Thread.getDefaultUncaughtExceptionHandler())
        val thread = Thread.currentThread()
        handler.uncaughtException(thread, throwable)
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

    /**
     * Asserts that a span was created with the expected crash details.
     */
    private fun SpanData.assertCrashSpanCaptured(
        thread: Thread,
        expectedExcType: String,
        expectedExcMessage: String? = null,
    ) {
        assertEquals("device.crash", name)
        assertEquals(StatusCode.ERROR, status.statusCode)

        // Check span attributes
        val attrs = attributes.asMap().mapKeys { it.key.key }
        assertEquals(expectedExcType, attrs[ExceptionAttributes.EXCEPTION_TYPE.key])
        assertEquals(expectedExcMessage, attrs[ExceptionAttributes.EXCEPTION_MESSAGE.key])
        assertEquals(thread.threadIdCompat, attrs[ThreadIncubatingAttributes.THREAD_ID.key])
        assertEquals(thread.name, attrs[ThreadIncubatingAttributes.THREAD_NAME.key])
        assertNotNull(attrs["heap.free"])
        assertNotNull(attrs["storage.free"])

        // Verify exception event exists
        val exceptionEvent = events.find { it.name == "exception" }
        assertNotNull("Expected exception event on span", exceptionEvent)
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
