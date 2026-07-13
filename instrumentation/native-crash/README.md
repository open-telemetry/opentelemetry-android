# Native Crash Instrumentation

Status: development

The native crash instrumentation replays a persisted native crash as an `app.crash` event when
the application next starts.

This first increment provides the module, persisted marker/context format, and replay path. Each
crash uses a separate marker so unreadable records can be retried without blocking other crashes.
Native signal capture is intentionally left for a follow-up change.

## Telemetry

The replayed event uses the original crash timestamp and includes:

* `exception.type`
* `exception.message`
* `session.id`, when available
* `service.version`, when available
* `os.name`
* `os.version`

The app and OS fields are persisted before a crash so the replayed event describes the process that
crashed rather than the process that reports it.

## Installation

Add the instrumentation dependency:

```kotlin
implementation("io.opentelemetry.android.instrumentation:native-crash:1.5.0-alpha")
```

The module is discovered and installed automatically when it is present on the runtime classpath.

## Limitations

Native signal handling, native stack unwinding, symbol upload, and symbolication are not included
in this increment. Until signal handling is added, this module only provides the replay side of the
native crash reporting flow. It does not emit native stack frames, so there is no native stack to
symbolicate in this change.

Each persisted crash marker is deleted immediately after its event is emitted. Replay is therefore
at most once per marker: if the application exits before the telemetry is exported, that crash
event may be lost.
