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
    private val signalHandlerInstaller: NativeSignalHandlerInstaller = JniNativeSignalHandlerInstaller(),
) : AndroidInstrumentation {
    override val name: String = "native-crash"

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val applicationContext = context.applicationContext
        val store = storeFactory(applicationContext)
        if (!signalHandlerInstaller.install(store.crashRecordPath)) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to install native crash signal handler")
        }

        executor.execute {
            val crashContext = applicationContext.currentCrashContext(openTelemetryRum)
            NativeCrashReporter(
                store = store,
                openTelemetryRum = openTelemetryRum,
            ).replayPreviousCrash()
            store.writeContext(crashContext)

            val sessionProvider = openTelemetryRum.sessionProvider
            if (sessionProvider is SessionPublisher) {
                sessionProvider.addObserver(NativeCrashSessionObserver(store, crashContext, executor))
            }
        }
    }
}

internal fun interface NativeSignalHandlerInstaller {
    fun install(crashRecordPath: File): Boolean
}

internal class JniNativeSignalHandlerInstaller(
    private val loadLibrary: (String) -> Unit = System::loadLibrary,
    private val nativeInstall: (String) -> Boolean = NativeCrashJni::install,
) : NativeSignalHandlerInstaller {
    override fun install(crashRecordPath: File): Boolean {
        if (!prepareCrashRecordDirectory(crashRecordPath)) {
            return false
        }
        return runCatching {
            loadLibrary(NATIVE_LIBRARY_NAME)
            nativeInstall(crashRecordPath.absolutePath)
        }.onFailure { error ->
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to load native crash signal handler", error)
        }.getOrDefault(false)
    }

    private companion object {
        const val NATIVE_LIBRARY_NAME = "otel_android_native_crash"
    }
}

internal fun prepareCrashRecordDirectory(crashRecordPath: File): Boolean {
    val directory = crashRecordPath.parentFile ?: return false
    return runCatching {
        directory.isDirectory || directory.mkdirs() || directory.isDirectory
    }.onFailure { error ->
        Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to prepare native crash marker directory", error)
    }.getOrDefault(false)
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
    fun replayPreviousCrash() {
        val crashContext = store.readContext()
        store.readCrashRecord()?.let { record -> replay(record, crashContext) }
    }

    private fun replay(
        record: NativeCrashRecord,
        crashContext: NativeCrashContext?,
    ) {
        val attributes = Attributes.builder()
        attributes.put(stringKey(EXCEPTION_TYPE), record.signalName)
        attributes.put(
            stringKey(EXCEPTION_MESSAGE),
            "Native crash signal ${record.signalName} (${record.signalNumber})",
        )
        crashContext?.addTo(attributes)

        openTelemetryRum.openTelemetry.logsBridge
            .loggerBuilder("io.opentelemetry.native-crash")
            .build()
            .logRecordBuilder()
            .setEventName(map("app.crash"))
            .setTimestamp(record.timestamp)
            .setAllAttributes(attributes.build())
            .emit()
        store.deleteCrashRecord()
    }
}

internal interface NativeCrashStore {
    val crashRecordPath: File

    fun readCrashRecord(): NativeCrashRecord?

    fun deleteCrashRecord()

    fun readContext(): NativeCrashContext?

    fun writeContext(context: NativeCrashContext)
}

internal class FileNativeCrashStore(
    private val directory: File,
) : NativeCrashStore {
    private val contextPath = File(directory, "native-crash-context.properties")
    override val crashRecordPath = File(directory, "native-crash-record.properties")

    override fun readCrashRecord(): NativeCrashRecord? {
        if (!crashRecordPath.isFile) {
            return null
        }
        val properties =
            try {
                crashRecordPath.readProperties()
            } catch (error: IllegalArgumentException) {
                deleteCrashRecord()
                return null
            } catch (error: IOException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash marker", error)
                deleteCrashRecord()
                return null
            } catch (error: SecurityException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash marker", error)
                deleteCrashRecord()
                return null
            }
        val record = properties.toCrashRecordOrNull()
        if (record == null) {
            deleteCrashRecord()
        }
        return record
    }

    override fun deleteCrashRecord() {
        runCatching {
            if (crashRecordPath.isFile && !crashRecordPath.delete()) {
                throw IOException("Failed to delete native crash marker")
            }
        }.onFailure { error ->
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to delete native crash marker", error)
        }
    }

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
        const val SIGNAL_NUMBER_KEY = "signal.number"
        const val TIMESTAMP_EPOCH_NANOS_KEY = "timestamp.epoch_nanos"
    }
}

internal data class NativeCrashRecord(
    val signalNumber: Int,
    val timestamp: Instant,
) {
    val signalName: String =
        when (signalNumber) {
            4 -> "SIGILL"
            5 -> "SIGTRAP"
            6 -> "SIGABRT"
            7 -> "SIGBUS"
            8 -> "SIGFPE"
            11 -> "SIGSEGV"
            13 -> "SIGPIPE"
            31 -> "SIGSYS"
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
