/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

/** A class representing all the details of an application crash. */
class CrashDetails internal constructor(
    /** Returns the thread that crashed.  */
    val thread: Thread,
    /** Returns the cause of the crash.  */
    val cause: Throwable,
)
