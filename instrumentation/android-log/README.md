
# Android Log Instrumentation

Status: development

The Android Log instrumentation transforms calls to `android.util.Log.x` to emit
OTel log record.

## Telemetry

This instrumentation produces the following telemetry:

### Log

* Type: Log
* Description: A log message
* Attributes:
    * `android.log.tag`: The tag passed to `android.uti.Log.x`
    * `exception.stacktrace` - Optional. See the ([semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-stacktrace)).
    * `exception.type` - Optional. See the ([semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-type))

## Installation

### Applying the Byte Buddy plugin

This instrumentation makes use of [Byte Buddy](https://bytebuddy.net/) to do bytecode instrumentation. Because of this, you need
to ensure that your application has the Byte Buddy plugin applied, as shown below.

```kotlin
plugins {
  id("net.bytebuddy.byte-buddy-gradle-plugin") version "LATEST_VERSION" // <1>
}
```

1. You can find the latest version [here](https://plugins.gradle.org/plugin/net.bytebuddy.byte-buddy-gradle-plugin).

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:android-log-library:1.0.0-rc.1-alpha")
byteBuddy("io.opentelemetry.android.instrumentation:android-log-agent:1.0.0-rc.1-alpha") // <2>
```
