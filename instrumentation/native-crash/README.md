# Native Crash Instrumentation

Status: development

The native crash instrumentation replays a persisted native crash as an `app.crash` event when
the application next starts.

This first increment provides the module, persisted marker/context format, and replay path. It uses
one marker for the most recent crash and a separate context snapshot maintained while the app is
running. Native signal capture is intentionally left for a follow-up change.

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

Add the instrumentation dependency:

```kotlin
implementation("io.opentelemetry.android.instrumentation:native-crash:1.5.0-alpha")
```

The module is discovered and installed automatically when it is present on the runtime classpath.

## Limitations

Native signal handling and native stack capture are not included in this increment. Until signal
handling is added, this module only provides the replay side of the native crash reporting flow.
It does not create or attach a binary crash dump. Symbol upload and symbolication are downstream
concerns and require a separate design once native stack frames are available.

The persisted crash marker is deleted immediately after its event is emitted. Replay is therefore
at most once: if the application exits before the telemetry is exported, that crash event may be
lost. A later change may add support for preserving multiple consecutive startup crashes.
Unreadable or malformed markers are discarded rather than retried.
