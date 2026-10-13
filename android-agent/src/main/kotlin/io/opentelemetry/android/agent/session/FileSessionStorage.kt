/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import android.util.AtomicFile
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.Session
import io.opentelemetry.api.trace.TraceId
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

@OptIn(Incubating::class)
internal class FileSessionStorage(
    file: File,
) : SessionStorage {
    private val file = AtomicFile(file)
    private var cached: Session = invalidSession
    private var loaded = false
    private var writable = true

    @Synchronized
    override fun get(): Session {
        if (!loaded) {
            loaded = true
            try {
                file.openRead().use { stream ->
                    val input = DataInputStream(stream)
                    val version = input.readInt()
                    if (version != VERSION) {
                        writable = false
                        return cached
                    }
                    val idBytes = ByteArray(32)
                    input.readFully(idBytes)
                    val id = idBytes.toString(Charsets.US_ASCII)
                    val start = input.readLong()
                    if (TraceId.isValid(id) && start >= 0 && input.read() == -1) {
                        cached = SessionImpl(id, start)
                    }
                }
            } catch (_: IOException) {
                // A missing or incomplete record starts a new chain.
            } catch (_: SecurityException) {
                writable = false
            }
        }
        return cached
    }

    @Synchronized
    override fun save(newSession: Session) {
        get()
        val id = newSession.id
        val start = newSession.startTimestamp
        if (!TraceId.isValid(id) || start < 0) return
        cached = SessionImpl(id, start)
        if (!writable) return
        try {
            val stream = file.startWrite()
            try {
                val output = DataOutputStream(stream)
                output.writeInt(VERSION)
                output.write(id.toByteArray(Charsets.US_ASCII))
                output.writeLong(start)
                output.flush()
                stream.fd.sync()
                file.finishWrite(stream)
            } catch (failure: IOException) {
                file.failWrite(stream)
                throw failure
            } catch (failure: SecurityException) {
                file.failWrite(stream)
                throw failure
            }
        } catch (_: IOException) {
            // Keep the in-memory session; a later save can retry the file.
        } catch (_: SecurityException) {
            // Storage failure must not interrupt telemetry.
        }
    }

    private companion object {
        const val VERSION = 1
    }
}
