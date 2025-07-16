# Startup Instrumentation

Status: development

The startup instrumentation provides initialization log events that describe the steps taken during
RUM initialization.

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:startup:LATEST_VERSION") // <1>
```

1. You can find the latest version [here](https://central.sonatype.com/artifact/io.opentelemetry.android.instrumentation/startup).
