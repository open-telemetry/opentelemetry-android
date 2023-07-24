/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation.crash;

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

    String spanName() {
        return getCause().getClass().getSimpleName();
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
