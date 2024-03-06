/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

/** A class representing all the details of an application crash. */
public final class CrashDetails {

    /** Creates a new {@link CrashDetails} instance. */
    public static CrashDetails create(Thread thread, Throwable cause) {
        return new CrashDetails(thread, cause);
    }

    private final Thread thread;
    private final Throwable cause;

    CrashDetails(Thread thread, Throwable cause) {
        this.thread = thread;
        this.cause = cause;
    }

    /** Returns the thread that crashed. */
    public Thread getThread() {
        return thread;
    }

    /** Returns the cause of the crash. */
    public Throwable getCause() {
        return cause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrashDetails that = (CrashDetails) o;

        if (!thread.equals(that.thread)) return false;
        return cause.equals(that.cause);
    }

    @Override
    public int hashCode() {
        int result = thread.hashCode();
        result = 31 * result + cause.hashCode();
        return result;
    }
}
