# Android Instrumentation for OkHttp version 3.0 and higher

Provides OpenTelemetry instrumentation for [okhttp3](https://square.github.io/okhttp/).

## Quickstart

### Add these dependencies to your project

Replace `OPENTELEMETRY_VERSION` with the [latest
release](https://search.maven.org/search?q=g:io.opentelemetry.android%20AND%20a:okhttp-3.0-library).

Replace `BYTEBUDDY_VERSION` with the [latest
release](https://search.maven.org/search?q=g:net.bytebuddy%20AND%20a:byte-buddy).

#### Byte buddy compilation plugin

This plugin leverages
Android's [Transform API](https://developer.android.com/reference/tools/gradle-api/current/com/android/build/api/variant/ScopedArtifactsOperation#toTransform(com.android.build.api.artifact.ScopedArtifact,kotlin.Function1,kotlin.Function1,kotlin.Function1))
to instrument bytecode at compile time. You can find more info on
its [repo page](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin/android-plugin.

```groovy
plugins {
    id 'net.bytebuddy.byte-buddy-gradle-plugin' version 'BYTEBUDDY_VERSION'
}
```

#### Project dependencies

```kotlin
implementation("io.opentelemetry.android:okhttp-3.0-library:OPENTELEMETRY_VERSION")
byteBuddy("io.opentelemetry.android:okhttp-3.0-agent:OPENTELEMETRY_VERSION")
```

After adding the plugin and the dependencies to your project, your OkHttp requests will be traced
automatically.

### Configuration

You can configure the automatic instrumentation by using the setters
in [OkHttpInstrumentationConfig](library/src/main/java/io/opentelemetry/instrumentation/library/okhttp/v3_0/OkHttpInstrumentationConfig.java)).
