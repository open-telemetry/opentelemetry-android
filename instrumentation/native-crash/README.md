# Native Crash Instrumentation

Status: development

The native crash instrumentation records fatal native signals and replays the persisted crash as an
`app.crash` event when the application next starts.

Each process launch has its own directory containing a crash marker and a context snapshot maintained
while the app is running. The signal handler records `SIGILL`, `SIGTRAP`, `SIGABRT`, `SIGBUS`, `SIGFPE`,
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

The app and OS fields are read from that launch's persisted crash-time context, so the replayed event
describes the process that crashed. The current launch writes to a separate directory.

## Installation

Building the native library requires CMake 3.22.1 or newer.

Add the instrumentation dependency:

```kotlin
implementation("io.opentelemetry.android.instrumentation:native-crash:1.7.0-alpha")
```

The module is discovered and installed automatically when it is present on the runtime classpath.
It persists the current process context and enables capture before replaying older launches on a
separate background executor. Session-context updates do not wait for replay. The legacy flat files
are replayed in place; they are never moved or overwritten by new capture.

At most eight previous launch directories are replayed per startup, newest first. Older directories
are pruned, so prolonged crash loops can discard the oldest pending reports. An I/O failure can leave
files for a later cleanup attempt. The active directory is never replayed or pruned.

## Limitations

Native stack capture is not included. Recovery only consumes a snapshot written by a compatible
future signal handler. Symbol upload and symbolication are downstream concerns.

Crashes that happen before native crash instrumentation finishes initialization are not recorded.

Recovery records a process-durable delivery claim before handing the event to OpenTelemetry. A
claimed crash is never emitted again, so replay is at most once and the event may be lost if the
process exits before export. Marker, snapshot, and cleanup failures are retried on later launches,
up to three attempts per phase and 24 hours from the first attempt. Pending recovery uses the old
launch's directory and does not disable capture for the current launch.

Unreadable recovery state is left untouched rather than guessing whether the crash was already
claimed. This does not use the attempt or age limits, but old directories are still subject to the
launch retention limit. Other reports can replay and new crashes can be captured independently.
Malformed recovery state, including an unknown format version, is discarded with the pending crash
because its delivery status cannot be proven.

This layout supports one instrumented app process. Separate directories do not provide coordination
between concurrently running app processes.
