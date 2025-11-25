# Android Instrumentation for OkHttp Websocket version 3.0 and higher

Status: development

The OpenTelemetry OkHttp WebSocket instrumentation for Android instruments
client-side OkHttp WebSocket usage. It creates telemetry for websocket lifecycle
events and can capture metadata about connections and messages.

Provides OpenTelemetry instrumentation for [okhttp3 websockets](https://square.github.io/okhttp/3.x/okhttp/okhttp3/WebSocket.html).

## Telemetry

This instrumentation primarily produces events/spans related to the WebSocket
connection lifecycle (connect, close, errors) and can record attributes such as
the peer host/port and any configured headers.

* Type: Span / Event (connection lifecycle)
* Name: Determined by instrumentation (e.g. `WebSocket connect` / `WebSocket close`)
* Attributes (examples):
  * `net.peer.name` — server host
  * `net.peer.port` — server port
  * `http.url` — the websocket URL
  * Captured headers per configuration

If a websocket connection errors, the instrumentation records the error on the
connection telemetry.

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
implementation("io.opentelemetry.android.instrumentation:okhttp3-websocket-library:1.0.0-rc.1-alpha")
byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-websocket-agent:1.0.0-rc.1-alpha")
```
