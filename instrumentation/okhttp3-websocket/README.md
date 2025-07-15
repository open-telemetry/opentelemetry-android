# Android Instrumentation for OkHttp Websocket version 3.0 and higher

## Status: development

Provides OpenTelemetry instrumentation for [okhttp3 websockets](https://square.github.io/okhttp/3.x/okhttp/okhttp3/WebSocket.html).

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
implementation("io.opentelemetry.android.instrumentation:okhttp3-websocket-library:LATEST_LIBRARY_VERSION") // <1>
byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-websocket-agent:LATEST_AGENT_VERSION") // <2>
```

1. You can find the latest library version [here](https://central.sonatype.com/artifact/io.opentelemetry.android.instrumentation/okhttp3-websocket-library).
2. You can find the latest instrumentation agent version [here](https://central.sonatype.com/artifact/io.opentelemetry.android.instrumentation/okhttp3-websocket-agent).
