/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(IncubatingApi::class)

package io.opentelemetry.android.instrumentation.nativecrash

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_NAME
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_VERSION
import io.opentelemetry.kotlin.semconv.ServiceAttributes.SERVICE_VERSION
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.util.Properties
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Entry point for replaying native crashes captured by a previous app process. */
@AutoService(AndroidInstrumentation::class)
class NativeCrashInstrumentation internal constructor(
    private val storeFactory: (Context) -> NativeCrashStore = { context ->
        FileNativeCrashStore(File(context.filesDir, "opentelemetry/native-crash"))
    },
    private val executor: Executor = Executors.newSingleThreadExecutor(),
) : AndroidInstrumentation {
    override val name: String = "native-crash"

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val applicationContext = context.applicationContext
        executor.execute {
            val store = storeFactory(applicationContext)
            val crashContext = applicationContext.currentCrashContext(openTelemetryRum)
            NativeCrashReporter(
                store = store,
                openTelemetryRum = openTelemetryRum,
            ).install(crashContext)

            val sessionProvider = openTelemetryRum.sessionProvider
            if (sessionProvider is SessionPublisher) {
                sessionProvider.addObserver(NativeCrashSessionObserver(store, crashContext, executor))
            }
        }
    }
}

internal class NativeCrashSessionObserver(
    private val store: NativeCrashStore,
    private val crashContext: NativeCrashContext,
    private val executor: Executor,
) : SessionObserver {
    override fun onSessionStarted(
        newSession: Session,
        previousSession: Session,
    ) {
        executor.execute {
            store.writeContext(crashContext.copy(sessionId = newSession.id))
        }
    }

    override fun onSessionEnded(session: Session) {}
}

internal class NativeCrashReporter(
    private val store: NativeCrashStore,
    private val openTelemetryRum: OpenTelemetryRum,
) {
    fun install(currentContext: NativeCrashContext) {
        replayPreviousCrashes()
        store.writeContext(currentContext)
    }

    private fun replayPreviousCrashes() {
        store.readCrashRecords().forEach(::replay)
    }

    private fun replay(record: NativeCrashRecord) {
        val attributes = Attributes.builder()
        attributes.put(stringKey(EXCEPTION_TYPE), record.signalName)
        attributes.put(
            stringKey(EXCEPTION_MESSAGE),
            "Native crash signal ${record.signalName} (${record.signalNumber})",
        )
        record.crashContext?.addTo(attributes)

        openTelemetryRum.openTelemetry.logsBridge
            .loggerBuilder("io.opentelemetry.native-crash")
            .build()
            .logRecordBuilder()
            .setEventName(map("app.crash"))
            .setTimestamp(record.timestamp)
            .setAllAttributes(attributes.build())
            .emit()
        store.deleteCrashRecord(record)
    }
}

internal interface NativeCrashStore {
    fun readCrashRecords(): List<NativeCrashRecord>

    fun deleteCrashRecord(record: NativeCrashRecord)

    fun readContext(): NativeCrashContext?

    fun writeContext(context: NativeCrashContext)
}

internal class FileNativeCrashStore(
    private val directory: File,
    private val crashRecordReader: (File) -> Properties = { path -> path.readProperties() },
) : NativeCrashStore {
    private val contextPath = File(directory, "native-crash-context.properties")

    override fun readCrashRecords(): List<NativeCrashRecord> {
        val paths =
            directory.listFiles { path ->
                path.isFile &&
                    path.name.startsWith(CRASH_RECORD_PREFIX) &&
                    path.name.endsWith(PROPERTIES_SUFFIX)
            } ?: return emptyList()

        return paths.sortedBy { it.name }.mapNotNull { path -> readCrashRecord(path) }
    }

    override fun deleteCrashRecord(record: NativeCrashRecord) {
        val markerName = record.markerName ?: return
        deleteCrashRecord(File(directory, markerName))
    }

    private fun readCrashRecord(path: File): NativeCrashRecord? {
        val properties =
            try {
                crashRecordReader(path)
            } catch (error: IllegalArgumentException) {
                deleteCrashRecord(path)
                return null
            } catch (error: IOException) {
                recordReadFailure(path, error)
                return null
            } catch (error: SecurityException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash marker", error)
                deleteCrashRecord(path)
                return null
            }
        deleteReadFailureCount(path)
        val record = properties.toCrashRecordOrNull()
        if (record == null) {
            deleteCrashRecord(path)
        }
        return record?.copy(markerName = path.name)
    }

    private fun deleteCrashRecord(path: File) {
        deleteFile(path, "native crash marker")
        deleteReadFailureCount(path)
    }

    private fun deleteReadFailureCount(path: File) {
        deleteFile(readFailurePath(path), "native crash marker retry state")
    }

    private fun deleteFile(
        path: File,
        description: String,
    ) {
        runCatching {
            if (path.isFile && !path.delete()) {
                throw IOException("Failed to delete $description")
            }
        }.onFailure { error ->
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to delete $description", error)
        }
    }

    private fun recordReadFailure(
        path: File,
        error: IOException,
    ) {
        Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash marker", error)
        val failurePath = readFailurePath(path)
        val failureCount =
            runCatching { failurePath.readText().toInt() }
                .getOrDefault(0) + 1
        if (failureCount >= MAX_READ_ATTEMPTS) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Discarding native crash marker after $failureCount failed read attempts",
            )
            deleteCrashRecord(path)
            return
        }

        runCatching { failurePath.writeText(failureCount.toString()) }
            .onFailure { retryError ->
                Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Failed to persist native crash marker retry state",
                    retryError,
                )
            }
    }

    private fun readFailurePath(path: File): File = File(directory, "${path.name}.read-failures")

    override fun readContext(): NativeCrashContext? {
        val properties = runCatching { contextPath.readProperties() }.getOrNull() ?: return null
        return properties.toCrashContextOrNull()
    }

    @Synchronized
    override fun writeContext(context: NativeCrashContext) {
        runCatching {
            directory.mkdirs()
            val properties = Properties()
            properties.setIfNotNull(SESSION_ID, context.sessionId)
            properties.setIfNotNull(SERVICE_VERSION, context.serviceVersion)
            properties.setIfNotNull(OS_NAME, context.osName)
            properties.setIfNotNull(OS_VERSION, context.osVersion)
            val temporaryPath = File(directory, "${contextPath.name}.tmp")
            try {
                FileOutputStream(temporaryPath).use { properties.store(it, null) }
                if (!temporaryPath.renameTo(contextPath)) {
                    throw IOException("Failed to replace native crash context")
                }
            } finally {
                temporaryPath.delete()
            }
        }.onFailure { error ->
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to persist native crash context", error)
        }
    }

    private fun Properties.toCrashRecordOrNull(): NativeCrashRecord? {
        return runCatching {
            val signalNumber =
                getProperty(SIGNAL_NUMBER_KEY)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: return null
            val timestamp =
                getProperty(TIMESTAMP_EPOCH_NANOS_KEY)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?.toInstant()
                    ?: return null
            NativeCrashRecord(
                signalNumber = signalNumber,
                timestamp = timestamp,
                crashContext = toCrashContextOrNull(),
            )
        }.getOrNull()
    }

    private fun Properties.toCrashContextOrNull(): NativeCrashContext? {
        val context =
            NativeCrashContext(
                sessionId = nonBlankProperty(SESSION_ID),
                serviceVersion = nonBlankProperty(SERVICE_VERSION),
                osName = nonBlankProperty(OS_NAME),
                osVersion = nonBlankProperty(OS_VERSION),
            )
        return context.takeUnless { it.isEmpty() }
    }

    private companion object {
        const val CRASH_RECORD_PREFIX = "native-crash-record-"
        const val PROPERTIES_SUFFIX = ".properties"
        const val SIGNAL_NUMBER_KEY = "signal.number"
        const val TIMESTAMP_EPOCH_NANOS_KEY = "timestamp.epoch_nanos"
        const val MAX_READ_ATTEMPTS = 3
    }
}

internal data class NativeCrashRecord(
    val signalNumber: Int,
    val timestamp: Instant,
    val crashContext: NativeCrashContext? = null,
    internal val markerName: String? = null,
) {
    val signalName: String =
        when (signalNumber) {
            4 -> "SIGILL"
            5 -> "SIGTRAP"
            6 -> "SIGABRT"
            7 -> "SIGBUS"
            8 -> "SIGFPE"
            11 -> "SIGSEGV"
            else -> "SIG$signalNumber"
        }
}

internal data class NativeCrashContext(
    val sessionId: String?,
    val serviceVersion: String?,
    val osName: String?,
    val osVersion: String?,
) {
    fun isEmpty(): Boolean =
        sessionId == null &&
            serviceVersion == null &&
            osName == null &&
            osVersion == null

    fun addTo(attributes: AttributesBuilder) {
        attributes.putIfNotNull(SESSION_ID, sessionId)
        attributes.putIfNotNull(SERVICE_VERSION, serviceVersion)
        attributes.putIfNotNull(OS_NAME, osName)
        attributes.putIfNotNull(OS_VERSION, osVersion)
    }
}

private fun Context.currentCrashContext(openTelemetryRum: OpenTelemetryRum): NativeCrashContext {
    val packageInfo = runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
    return NativeCrashContext(
        sessionId = openTelemetryRum.sessionProvider.getSessionId().takeIf { it.isNotBlank() },
        serviceVersion = packageInfo?.versionName,
        osName = "Android",
        osVersion = Build.VERSION.RELEASE,
    )
}

private fun AttributesBuilder.putIfNotNull(
    key: String,
    value: String?,
) {
    value?.takeIf { it.isNotBlank() }?.let { put(stringKey(key), it) }
}

private fun Properties.setIfNotNull(
    key: String,
    value: String?,
) {
    value?.takeIf { it.isNotBlank() }?.let { setProperty(key, it) }
}

private fun Properties.nonBlankProperty(key: String): String? = getProperty(key)?.takeIf { it.isNotBlank() }

private fun File.readProperties(): Properties =
    Properties().also { properties ->
        FileInputStream(this).use { properties.load(it) }
    }

private const val NANOS_PER_SECOND = 1_000_000_000L

private fun Long.toInstant(): Instant = Instant.ofEpochSecond(Math.floorDiv(this, NANOS_PER_SECOND), Math.floorMod(this, NANOS_PER_SECOND))
