# Android Instrumentation for OkHttp version 3.0 and higher

Status: development

The OpenTelemetry OkHttp instrumentation for Android instruments client-side requests
made via OkHttp (version 3.0 +) [okhttp3](https://square.github.io/okhttp/). It adds distributed tracing context,
creates client HTTP spans, and records request/response metadata.

## Telemetry

This instrumentation produces HTTP client spans using the OpenTelemetry [HTTP semantic
conventions](https://opentelemetry.io/docs/specs/semconv/http/http-spans/). The span name is created via the HTTP span name extractor and attributes
are provided by the OkHttp attributes getter.

### HTTP client span

* Type: Span
* Name: Determined by the HTTP span name extractor (typically `HTTP {method}` or derived from the URL)
* Description: Client-side HTTP request
* Common attributes (following OpenTelemetry HTTP semantic conventions):
  * `http.method` — request method (GET, POST, etc.)
  * `http.url` — full URL
  * `http.status_code` — response status code
  * `http.flavor` — protocol (e.g. `1.1`)
  * `net.peer.name` — server host
  * `net.peer.port` — server port
  * Captured request/response headers per configuration

If a request fails, the span is ended and the error is recorded.

## Quickstart

### Add these dependencies to your project

Replace `BYTEBUDDY_VERSION` with the [latest
release](https://central.sonatype.com/artifact/net.bytebuddy/byte-buddy-gradle-plugin/versions).

#### Byte buddy compilation plugin

This plugin leverages
Android's [Transform API](https://developer.android.com/reference/tools/gradle-api/current/com/android/build/api/variant/ScopedArtifactsOperation#toTransform(com.android.build.api.artifact.ScopedArtifact,kotlin.Function1,kotlin.Function1,kotlin.Function1))
to instrument bytecode at compile time. You can find more info on
its [repo page](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin/android-plugin).

```groovy
plugins {
    id 'net.bytebuddy.byte-buddy-gradle-plugin' version 'BYTEBUDDY_VERSION'
}
```

#### Project dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:okhttp3-library:1.0.0-rc.1-alpha")
byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-agent:1.0.0-rc.1-alpha")
```

After adding the plugin and the dependencies to your project, your OkHttp requests will be traced
automatically.

### Configuration

You can configure the automatic instrumentation by using the setters
from
the [OkHttpInstrumentation](library/src/main/java/io/opentelemetry/instrumentation/library/okhttp/v3_0/OkHttpInstrumentation.kt)
instance provided via the AndroidInstrumentationLoader as shown below:

```java
OkHttpInstrumentation instrumentation = AndroidInstrumentationLoader.getInstrumentation(OkHttpInstrumentation.class);

// Call `instrumentation` setters.
```

> [!NOTE]
> You must make sure to apply any configurations **before** initializing your OpenTelemetryRum
> instance (i.e. calling OpenTelemetryRum.builder()...build()). Otherwise your configs won't be
> taken into account during the RUM initialization process.
