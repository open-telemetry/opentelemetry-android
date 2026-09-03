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
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.semconv.internal.SemconvCompat.Companion.map
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
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
import java.nio.channels.FileChannel
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
        executor.execute {
            val store = storeFactory(applicationContext)
            val crashContext = applicationContext.currentCrashContext(openTelemetryRum)
            val recoveryResult =
                NativeCrashReporter(
                    store = store,
                    openTelemetryRum = openTelemetryRum,
                ).replayPreviousCrash()
            if (recoveryResult == NativeCrashRecoveryResult.RETRY_PENDING) {
                Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Native crash signal handler disabled while crash recovery is pending",
                )
                return@execute
            }
            if (!store.writeContext(crashContext)) {
                Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Native crash signal handler disabled because crash context could not be persisted",
                )
                return@execute
            }

            val sessionProvider = openTelemetryRum.sessionProvider
            if (sessionProvider is SessionPublisher) {
                sessionProvider.addObserver(NativeCrashSessionObserver(store, crashContext, executor))
            }

            if (!signalHandlerInstaller.install(store.crashRecordPath)) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to install native crash signal handler")
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
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun replayPreviousCrash(): NativeCrashRecoveryResult =
        synchronized(processRecoveryLock) {
            val fileLock = store.acquireRecoveryLock() ?: return@synchronized NativeCrashRecoveryResult.RETRY_PENDING
            try {
                recover()
            } catch (error: Exception) {
                abandonUnexpectedFailure(error)
            } catch (error: LinkageError) {
                abandonUnexpectedFailure(error)
            } finally {
                try {
                    fileLock.close()
                } catch (error: Exception) {
                    logReplayFailure(error)
                } catch (error: LinkageError) {
                    logReplayFailure(error)
                }
            }
        }

    private fun recover(): NativeCrashRecoveryResult {
        val state =
            when (val stateRead = store.readRecoveryState()) {
                is NativeCrashRead.Success -> stateRead.value
                NativeCrashRead.Missing -> null
                NativeCrashRead.Malformed -> return discardUnreadableState()
                NativeCrashRead.Failed -> return NativeCrashRecoveryResult.RETRY_PENDING
            }
        return when (val markerRead = store.readCrashRecordForRecovery()) {
            is NativeCrashRead.Success -> recover(markerRead.value, state)

            NativeCrashRead.Missing,
            NativeCrashRead.Malformed,
            -> cleanup(state?.asCleanup() ?: newState(NativeCrashRecoveryPhase.CLEANUP))

            NativeCrashRead.Failed -> recoverMarkerFailure(state)
        }
    }

    private fun recover(
        record: NativeCrashRecord,
        state: NativeCrashRecoveryState?,
    ): NativeCrashRecoveryResult {
        if (state != null && state.appliesTo(record)) {
            when (state.phase) {
                NativeCrashRecoveryPhase.DELIVERY_CLAIMED,
                NativeCrashRecoveryPhase.CLEANUP,
                -> return cleanup(state.asCleanup())

                NativeCrashRecoveryPhase.ABANDONED -> return cleanupAbandoned(state, record)

                else -> Unit
            }
        }

        val snapshot =
            when (val snapshotRead = store.readCrashSnapshotForRecovery(record)) {
                is NativeCrashRead.Success -> {
                    snapshotRead.value
                }

                NativeCrashRead.Missing,
                NativeCrashRead.Malformed,
                -> {
                    null
                }

                NativeCrashRead.Failed -> {
                    val retry = nextRetry(NativeCrashRecoveryPhase.SNAPSHOT_READ, state, record)
                    if (!retry.isExhausted()) {
                        return persistRetry(retry)
                    }
                    null
                }
            }

        val claim = newState(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, record, state)
        if (!store.writeRecoveryState(claim)) return abandon(claim)
        try {
            replay(record, store.readContext(), snapshot)
        } catch (error: Exception) {
            logReplayFailure(error)
        } catch (error: LinkageError) {
            logReplayFailure(error)
        }
        return cleanup(claim)
    }

    private fun replay(
        record: NativeCrashRecord,
        crashContext: NativeCrashContext?,
        snapshot: NativeCrashSnapshot?,
    ) {
        val attributes = Attributes.builder()
        attributes.put(stringKey(EXCEPTION_TYPE), record.signalName)
        attributes.put(
            stringKey(EXCEPTION_MESSAGE),
            "Native crash signal ${record.signalName} (${record.signalNumber})",
        )
        snapshot
            ?.let(::recoverFrames)
            ?.takeIf { it.isNotEmpty() }
            ?.let { attributes.put(stringKey(EXCEPTION_STACKTRACE), it.toStackTrace(snapshot.architecture)) }
        crashContext?.addTo(attributes)

        openTelemetryRum.openTelemetry.logsBridge
            .loggerBuilder("io.opentelemetry.native-crash")
            .build()
            .logRecordBuilder()
            .setEventName(map("app.crash"))
            .setTimestamp(record.timestamp)
            .setAllAttributes(attributes.build())
            .emit()
    }

    private fun recoverFrames(snapshot: NativeCrashSnapshot): List<NativeCrashFrame> =
        try {
            NativeCrashSnapshotUnwinder.unwind(snapshot)
        } catch (error: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to unwind native crash snapshot", error)
            emptyList()
        } catch (error: LinkageError) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to unwind native crash snapshot", error)
            emptyList()
        }

    private fun List<NativeCrashFrame>.toStackTrace(architecture: NativeCrashArchitecture): String =
        mapIndexed { index, frame ->
            buildString {
                append("#${index.toString().padStart(2, '0')} pc ")
                append(frame.moduleRelativeAddress.toString(16).padStart(architecture.pointerSize * 2, '0'))
                append("  ${frame.moduleName}")
                frame.buildId?.let { append(" (BuildId: $it)") }
            }
        }.joinToString("\n")

    private fun recoverMarkerFailure(state: NativeCrashRecoveryState?): NativeCrashRecoveryResult {
        if (state?.phase == NativeCrashRecoveryPhase.ABANDONED) return cleanupAbandoned(state)
        if (state?.phase == NativeCrashRecoveryPhase.DELIVERY_CLAIMED ||
            state?.phase == NativeCrashRecoveryPhase.CLEANUP
        ) {
            return cleanup(state.asCleanup())
        }
        val retry = nextRetry(NativeCrashRecoveryPhase.MARKER_READ, state)
        return if (retry.isExhausted()) abandon(retry) else persistRetry(retry)
    }

    private fun nextRetry(
        phase: NativeCrashRecoveryPhase,
        state: NativeCrashRecoveryState?,
        record: NativeCrashRecord? = null,
    ): NativeCrashRecoveryState {
        val matching =
            state?.takeIf {
                it.phase == phase &&
                    ((record == null && !it.hasIdentity()) || (record != null && it.matches(record)))
            }
        return (matching ?: newState(phase, record, state)).copy(attempts = (matching?.attempts ?: 0) + 1)
    }

    private fun persistRetry(state: NativeCrashRecoveryState): NativeCrashRecoveryResult =
        if (store.writeRecoveryState(state)) NativeCrashRecoveryResult.RETRY_PENDING else abandon(state)

    private fun cleanup(state: NativeCrashRecoveryState): NativeCrashRecoveryResult {
        if (store.deleteCrashFiles() && store.deleteRecoveryState()) return NativeCrashRecoveryResult.COMPLETE
        val retry = state.asCleanup().copy(attempts = state.attempts + 1)
        return if (retry.isExhausted()) abandon(retry) else persistRetry(retry)
    }

    private fun abandon(state: NativeCrashRecoveryState): NativeCrashRecoveryResult {
        store.writeRecoveryState(state.copy(phase = NativeCrashRecoveryPhase.ABANDONED))
        val crashFilesDeleted = store.deleteCrashFiles()
        if (crashFilesDeleted) store.deleteRecoveryState()
        return NativeCrashRecoveryResult.COMPLETE
    }

    private fun cleanupAbandoned(
        state: NativeCrashRecoveryState,
        record: NativeCrashRecord? = null,
    ): NativeCrashRecoveryResult {
        if (record != null && !state.hasIdentity()) {
            store.writeRecoveryState(
                newState(NativeCrashRecoveryPhase.ABANDONED, record, state).copy(attempts = state.attempts),
            )
        }
        if (store.deleteCrashFiles()) store.deleteRecoveryState()
        return NativeCrashRecoveryResult.COMPLETE
    }

    private fun discardUnreadableState(): NativeCrashRecoveryResult = abandon(newState(NativeCrashRecoveryPhase.ABANDONED))

    private fun abandonUnexpectedFailure(error: Throwable): NativeCrashRecoveryResult {
        logReplayFailure(error)
        return abandon(newState(NativeCrashRecoveryPhase.ABANDONED))
    }

    private fun newState(
        phase: NativeCrashRecoveryPhase,
        record: NativeCrashRecord? = null,
        previousState: NativeCrashRecoveryState? = null,
    ): NativeCrashRecoveryState =
        NativeCrashRecoveryState.create(
            phase,
            previousState
                ?.takeIf {
                    if (record == null) !it.hasIdentity() else it.appliesTo(record)
                }?.firstAttemptEpochMillis ?: nowMillis(),
            record,
        )

    private fun NativeCrashRecoveryState.asCleanup(): NativeCrashRecoveryState = copy(phase = NativeCrashRecoveryPhase.CLEANUP)

    private fun NativeCrashRecoveryState.isExhausted(): Boolean {
        val now = nowMillis()
        return attempts >= MAX_ATTEMPTS ||
            (now >= firstAttemptEpochMillis && now - firstAttemptEpochMillis >= MAX_RETRY_AGE_MILLIS)
    }

    private fun logReplayFailure(error: Throwable) {
        Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to replay native crash", error)
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MAX_RETRY_AGE_MILLIS = 24 * 60 * 60 * 1_000L
        val processRecoveryLock = Any()
    }
}

internal interface NativeCrashStore {
    val crashRecordPath: File

    val crashSnapshotPath: File

    fun readCrashRecord(): NativeCrashRecord?

    fun readCrashRecordForRecovery(): NativeCrashRead<NativeCrashRecord>

    fun readCrashSnapshot(record: NativeCrashRecord): NativeCrashSnapshot?

    fun readCrashSnapshotForRecovery(record: NativeCrashRecord): NativeCrashRead<NativeCrashSnapshot>

    fun readRecoveryState(): NativeCrashRead<NativeCrashRecoveryState>

    fun writeRecoveryState(state: NativeCrashRecoveryState): Boolean

    fun acquireRecoveryLock(): NativeCrashRecoveryLock?

    fun deleteCrashRecord()

    fun deleteCrashSnapshot(): Boolean

    fun deleteCrashFiles(): Boolean

    fun deleteRecoveryState(): Boolean

    fun readContext(): NativeCrashContext?

    fun writeContext(context: NativeCrashContext): Boolean
}

internal class FileNativeCrashStore(
    private val directory: File,
) : NativeCrashStore {
    private val contextPath = File(directory, "native-crash-context.properties")
    private val recoveryStatePath = File(directory, "native-crash-recovery.properties")
    private val recoveryLockPath = File(directory, "native-crash-recovery.lock")
    override val crashRecordPath = File(directory, "native-crash-record.properties")
    override val crashSnapshotPath = File(directory, "native-crash-snapshot.bin")

    override fun readCrashRecord(): NativeCrashRecord? {
        val result = readCrashRecordForRecovery()
        if (result is NativeCrashRead.Success) return result.value
        if (result != NativeCrashRead.Missing) deleteCrashFiles()
        return null
    }

    override fun readCrashRecordForRecovery(): NativeCrashRead<NativeCrashRecord> {
        if (!crashRecordPath.isFile) return NativeCrashRead.Missing
        val properties =
            try {
                crashRecordPath.readProperties()
            } catch (error: IllegalArgumentException) {
                return NativeCrashRead.Malformed
            } catch (error: IOException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash marker", error)
                return NativeCrashRead.Failed
            } catch (error: SecurityException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash marker", error)
                return NativeCrashRead.Failed
            }
        return properties.toCrashRecordOrNull()?.let { NativeCrashRead.Success(it) }
            ?: NativeCrashRead.Malformed
    }

    override fun readCrashSnapshot(record: NativeCrashRecord): NativeCrashSnapshot? {
        val result =
            try {
                readCrashSnapshotForRecovery(record)
            } catch (error: Exception) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash snapshot", error)
                NativeCrashRead.Failed
            } catch (error: LinkageError) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash snapshot", error)
                NativeCrashRead.Failed
            }
        if (result is NativeCrashRead.Success) return result.value
        if (result != NativeCrashRead.Missing) deleteCrashSnapshot()
        return null
    }

    override fun readCrashSnapshotForRecovery(record: NativeCrashRecord): NativeCrashRead<NativeCrashSnapshot> {
        if (!crashSnapshotPath.isFile) return NativeCrashRead.Missing
        val bytes =
            try {
                crashSnapshotPath.readBytes()
            } catch (error: IOException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash snapshot", error)
                return NativeCrashRead.Failed
            } catch (error: SecurityException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash snapshot", error)
                return NativeCrashRead.Failed
            }
        return NativeCrashSnapshotParser.parse(bytes, record)?.let { NativeCrashRead.Success(it) }
            ?: NativeCrashRead.Malformed
    }

    override fun readRecoveryState(): NativeCrashRead<NativeCrashRecoveryState> {
        if (!recoveryStatePath.isFile) return NativeCrashRead.Missing
        val properties =
            try {
                recoveryStatePath.readProperties()
            } catch (error: IllegalArgumentException) {
                return NativeCrashRead.Malformed
            } catch (error: IOException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash recovery state", error)
                return NativeCrashRead.Failed
            } catch (error: SecurityException) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to read native crash recovery state", error)
                return NativeCrashRead.Failed
            }
        return properties.toRecoveryStateOrNull()?.let { NativeCrashRead.Success(it) }
            ?: NativeCrashRead.Malformed
    }

    @Synchronized
    override fun writeRecoveryState(state: NativeCrashRecoveryState): Boolean = writeRecoveryStateFile(state.toProperties())

    override fun acquireRecoveryLock(): NativeCrashRecoveryLock? {
        val channel =
            try {
                if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                    throw IOException("Failed to create native crash directory")
                }
                FileOutputStream(recoveryLockPath, true).channel
            } catch (error: Exception) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to open native crash recovery lock", error)
                return null
            } catch (error: LinkageError) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to open native crash recovery lock", error)
                return null
            }
        return try {
            // Recovery owns fixed marker and snapshot paths until cleanup completes. Wait on this
            // background executor so another process cannot install a handler and overwrite them.
            val lock = channel.lock()
            NativeCrashRecoveryLock {
                try {
                    lock.release()
                } catch (error: Exception) {
                    Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to release native crash recovery lock", error)
                } catch (error: LinkageError) {
                    Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to release native crash recovery lock", error)
                } finally {
                    closeRecoveryChannel(channel)
                }
            }
        } catch (error: Exception) {
            closeRecoveryChannel(channel)
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to acquire native crash recovery lock", error)
            null
        } catch (error: LinkageError) {
            closeRecoveryChannel(channel)
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to acquire native crash recovery lock", error)
            null
        }
    }

    override fun deleteCrashRecord() {
        deleteFile(crashRecordPath, "native crash marker")
    }

    override fun deleteCrashSnapshot(): Boolean = deleteFile(crashSnapshotPath, "native crash snapshot")

    override fun deleteCrashFiles(): Boolean {
        val markerDeleted = deleteFile(crashRecordPath, "native crash marker")
        val snapshotDeleted = deleteCrashSnapshot()
        return markerDeleted && snapshotDeleted
    }

    override fun deleteRecoveryState(): Boolean = deleteFile(recoveryStatePath, "native crash recovery state")

    override fun readContext(): NativeCrashContext? {
        val properties = runCatching { contextPath.readProperties() }.getOrNull() ?: return null
        return properties.toCrashContextOrNull()
    }

    @Synchronized
    override fun writeContext(context: NativeCrashContext): Boolean =
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
                val replaced =
                    temporaryPath.renameTo(contextPath) ||
                        (contextPath.isFile && contextPath.delete() && temporaryPath.renameTo(contextPath))
                if (!replaced) {
                    throw IOException("Failed to replace native crash context")
                }
            } finally {
                temporaryPath.delete()
            }
        }.onFailure { error ->
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to persist native crash context", error)
        }.isSuccess

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

    private fun Properties.toRecoveryStateOrNull(): NativeCrashRecoveryState? {
        if (getProperty(RECOVERY_VERSION_KEY) != RECOVERY_VERSION.toString()) return null
        val phase =
            getProperty(RECOVERY_PHASE_KEY)
                ?.let { value -> NativeCrashRecoveryPhase.entries.firstOrNull { it.name == value } }
                ?: return null
        val attempts = getProperty(RECOVERY_ATTEMPTS_KEY)?.toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val firstAttempt =
            getProperty(RECOVERY_FIRST_ATTEMPT_KEY)?.toLongOrNull()?.takeIf { it > 0 }
                ?: return null
        val signalNumber = getProperty(SIGNAL_NUMBER_KEY)?.toIntOrNull()?.takeIf { it > 0 }
        val timestampSecond = getProperty(RECOVERY_TIMESTAMP_SECOND_KEY)?.toLongOrNull()?.takeIf { it >= 0 }
        val timestampNano = getProperty(RECOVERY_TIMESTAMP_NANO_KEY)?.toIntOrNull()?.takeIf { it in 0..999_999_999 }
        val identityFields = listOf(signalNumber, timestampSecond, timestampNano).count { it != null }
        if (identityFields != 0 && identityFields != 3) return null
        if (phase == NativeCrashRecoveryPhase.MARKER_READ && identityFields != 0) return null
        if (phase in IDENTITY_REQUIRED_PHASES && identityFields != 3) return null
        return NativeCrashRecoveryState(
            phase = phase,
            attempts = attempts,
            firstAttemptEpochMillis = firstAttempt,
            signalNumber = signalNumber,
            timestampEpochSecond = timestampSecond,
            timestampNano = timestampNano,
        )
    }

    private fun NativeCrashRecoveryState.toProperties(): Properties =
        Properties().also { properties ->
            properties.setProperty(RECOVERY_VERSION_KEY, RECOVERY_VERSION.toString())
            properties.setProperty(RECOVERY_PHASE_KEY, phase.name)
            properties.setProperty(RECOVERY_ATTEMPTS_KEY, attempts.toString())
            properties.setProperty(RECOVERY_FIRST_ATTEMPT_KEY, firstAttemptEpochMillis.toString())
            signalNumber?.let { properties.setProperty(SIGNAL_NUMBER_KEY, it.toString()) }
            timestampEpochSecond?.let { properties.setProperty(RECOVERY_TIMESTAMP_SECOND_KEY, it.toString()) }
            timestampNano?.let { properties.setProperty(RECOVERY_TIMESTAMP_NANO_KEY, it.toString()) }
        }

    private fun writeRecoveryStateFile(properties: Properties): Boolean =
        try {
            if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                throw IOException("Failed to create native crash directory")
            }
            val temporaryPath = File(directory, "${recoveryStatePath.name}.tmp")
            try {
                FileOutputStream(temporaryPath).use {
                    properties.store(it, null)
                    it.fd.sync()
                }
                val replaced = temporaryPath.renameTo(recoveryStatePath)
                if (!replaced) throw IOException("Failed to replace ${recoveryStatePath.name}")
            } finally {
                temporaryPath.delete()
            }
            true
        } catch (error: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to persist native crash recovery state", error)
            false
        } catch (error: LinkageError) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to persist native crash recovery state", error)
            false
        }

    private fun deleteFile(
        file: File,
        description: String,
    ): Boolean =
        try {
            if (file.exists() && !file.delete()) throw IOException("Failed to delete $description")
            true
        } catch (error: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to delete $description", error)
            false
        } catch (error: LinkageError) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to delete $description", error)
            false
        }

    private fun closeRecoveryChannel(channel: FileChannel) {
        try {
            channel.close()
        } catch (error: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to close native crash recovery lock", error)
        } catch (error: LinkageError) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to close native crash recovery lock", error)
        }
    }

    private companion object {
        const val RECOVERY_VERSION = 1
        const val SIGNAL_NUMBER_KEY = "signal.number"
        const val TIMESTAMP_EPOCH_NANOS_KEY = "timestamp.epoch_nanos"
        const val RECOVERY_VERSION_KEY = "recovery.version"
        const val RECOVERY_PHASE_KEY = "recovery.phase"
        const val RECOVERY_ATTEMPTS_KEY = "recovery.attempts"
        const val RECOVERY_FIRST_ATTEMPT_KEY = "recovery.first_attempt_epoch_millis"
        const val RECOVERY_TIMESTAMP_SECOND_KEY = "recovery.timestamp_epoch_second"
        const val RECOVERY_TIMESTAMP_NANO_KEY = "recovery.timestamp_nano"
        val IDENTITY_REQUIRED_PHASES =
            setOf(
                NativeCrashRecoveryPhase.SNAPSHOT_READ,
                NativeCrashRecoveryPhase.DELIVERY_CLAIMED,
            )
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
