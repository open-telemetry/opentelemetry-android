# Native Crash Instrumentation

Status: development

The native crash instrumentation records fatal native signals and replays the persisted crash as an
`app.crash` event when the application next starts.

It uses one marker for the most recent crash and a separate context snapshot maintained while the
app is running. The signal handler records `SIGILL`, `SIGTRAP`, `SIGABRT`, `SIGBUS`, `SIGFPE`,
`SIGSEGV`, and `SIGSYS`, then restores and invokes the handler that was installed before this
instrumentation. Signals that were already ignored remain ignored.

## Persisted marker format

The native handler writes the marker as UTF-8 text with a trailing newline:

```properties
signal.number=<positive integer>
timestamp.epoch_nanos=<positive integer>
```

The native writer and Kotlin reader must keep these keys and value formats in sync.

## Telemetry

The replayed event uses the original crash timestamp and includes:

* `exception.type`
* `exception.message`
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
implementation("io.opentelemetry.android.instrumentation:native-crash:1.5.0-alpha")
```

The module is discovered and installed automatically when it is present on the runtime classpath.

## Limitations

Native stack capture is not included. This module does not create or attach a binary crash dump.
Symbol upload and symbolication are downstream concerns and require a separate design once native
stack frames are available.

The persisted crash marker is deleted immediately after its event is emitted. Replay is therefore
at most once: if the application exits before the telemetry is exported, that crash event may be
lost. A later change may add support for preserving multiple consecutive startup crashes.
Unreadable or malformed markers are discarded rather than retried.
