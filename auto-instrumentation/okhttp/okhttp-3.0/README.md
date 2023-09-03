# Android Instrumentation for OkHttp version 3.0 and higher

Provides OpenTelemetry instrumentation for [okhttp3](https://square.github.io/okhttp/).

## Quickstart

### Add these dependencies to your project

Replace `OPENTELEMETRY_VERSION` with the [latest
release](https://search.maven.org/search?q=g:io.opentelemetry.instrumentation%20AND%20a:opentelemetry-okhttp-3.0).

Replace `BYTEBUDDY_VERSION` with the [latest
release](https://search.maven.org/search?q=g:io.opentelemetry.instrumentation%20AND%20a:opentelemetry-okhttp-3.0).

#### Byte buddy compilation plugin

This plugin leverages
Android's [ASM API](https://developer.android.com/reference/tools/gradle-api/8.0/com/android/build/api/instrumentation/AsmClassVisitorFactory)
to instrument bytecode at compile time. You can find more info on
its [repo page](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin/android-plugin.

```groovy
plugins {
    id 'net.bytebuddy.byte-buddy-gradle-plugin' version 'BYTEBUDDY_VERSION'
}
```

#### Project dependencies

```groovy
implementation "io.opentelemetry.android:okhttp-3.0-library:OPENTELEMETRY_VERSION"
byteBuddy "io.opentelemetry.android:okhttp-3.0-agent:OPENTELEMETRY_VERSION"
```

After adding the plugin and the dependencies in your project, you'll automatically get your OkHttp
requests traced.

### Configuration

You can configure the automatic instrumentation by...
