# Native Crash Instrumentation

Status: development

The native crash instrumentation records fatal native signals and replays the persisted crash as an
`app.crash` event when the application next starts.

It uses one marker for the most recent crash and a separate context snapshot maintained while the
app is running. The signal handler records `SIGILL`, `SIGTRAP`, `SIGABRT`, `SIGBUS`, `SIGFPE`,
`SIGSEGV`, and `SIGSYS`, then restores the previous action for that signal and re-delivers it
through the kernel. This preserves the previous handler's signal mask, flags, and available fault
details without changing registrations for the other signals. Signals that were already ignored
remain ignored.

## Persisted marker format

The native handler writes the marker as UTF-8 text with a trailing newline:

```properties
signal.number=<positive integer>
timestamp.epoch_nanos=<positive integer>
```

The native writer and Kotlin reader must keep these keys and value formats in sync.

The versioned binary format used for native frame recovery is documented in
[`SNAPSHOT_FORMAT.md`](SNAPSHOT_FORMAT.md). Runtime snapshot capture remains follow-up work.

## Telemetry

The replayed event uses the original crash timestamp and includes:

* `exception.type`
* `exception.message`
* `exception.stacktrace`, when a matching snapshot contains recoverable frames
* `session.id`, when available
* `service.version`, when available
* `os.name`
* `os.version`

The app and OS fields are read from the persisted crash-time context before it is replaced with the
new process context, so the replayed event describes the process that crashed.

## Installation

Building the native library requires CMake 3.22.1 or newer.

Add the instrumentation dependency:

```kotlin
implementation("io.opentelemetry.android.instrumentation:native-crash:1.6.0-alpha")
```

The module is discovered and installed automatically when it is present on the runtime classpath.
It replays any marker from the previous process and persists the current process context before
enabling the native signal handler.

## Limitations

Native stack capture is not included. Recovery only consumes a snapshot written by a compatible
future signal handler. Symbol upload and symbolication are downstream concerns.

Crashes that happen before native crash instrumentation finishes initialization are not recorded.

Recovery records a process-durable delivery claim before handing the event to OpenTelemetry. A
claimed crash is never emitted again, so replay is at most once and the event may be lost if the
process exits before export. Marker, snapshot, and cleanup failures are retried on later launches,
up to three attempts per phase and 24 hours from the first attempt. The signal handler stays
disabled while a retry is pending so it cannot overwrite the files being recovered.

Recovery waits for the fixed marker and snapshot paths when another app process is using them. If
the process lock cannot be acquired, or the recovery state cannot be read while a crash may be
pending, the handler remains disabled rather than guessing whether the crash was already claimed.
Malformed recovery state is discarded with the pending crash because ownership cannot be proven.

Only one crash can be pending. Supporting multiple consecutive startup crashes requires per-crash
paths and remains follow-up work.
