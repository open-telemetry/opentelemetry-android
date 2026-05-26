# Writing Instrumentation

This document describes how to add automatic instrumentation to OpenTelemetry Android.
It is intended for contributors creating new modules under `instrumentation/`.

OpenTelemetry Android instrumentations are Android libraries that implement
[`AndroidInstrumentation`](../instrumentation/android-instrumentation/src/main/java/io/opentelemetry/android/instrumentation/AndroidInstrumentation.kt). Some instrumentations can attach themselves with normal Android or library APIs. Others
need ByteBuddy build-time weaving because the target API does not expose a hook that can
be registered at runtime. The module layout depends on which case you are building.

## Choose The Instrumentation Shape

Instrumentation implementations can be split into two categories:

- Regular instrumentation
- ByteBuddy instrumentation

Use a regular instrumentation module when the code can attach itself through existing
runtime APIs, for example:

- Android framework callbacks such as activity or fragment lifecycle callbacks.
- Listener, observer, callback, interceptor, or wrapper APIs exposed by the target
  Android or library API.
- Telemetry that can be started from the instrumentation's `install()` method.

Use [ByteBuddy](https://bytebuddy.net/) when the target behavior cannot be reached automatically through runtime
registration alone, for example:

- Calls to static APIs such as `android.util.Log`.
- Third-party clients where automatic setup requires modifying application bytecode.
- APIs where manual user setup would be required unless the build rewrites calls or
  constructors.

ByteBuddy support in this repository is build-time Android bytecode instrumentation. It
is not the JVM Java agent model. The application applies the [ByteBuddy Gradle plugin](https://github.com/raphw/byte-buddy/blob/master/byte-buddy-gradle-plugin/android-plugin/README.md) and
declares the instrumentation's `agent` artifact in the `byteBuddy(...)` configuration, which will be used during the Android app compilation to do bytecode weaving.

## Regular Instrumentation Layout

A regular instrumentation is one Gradle module below `instrumentation/`:

```text
instrumentation/
  my-feature/
    build.gradle.kts
    README.md
    src/main/kotlin/io/opentelemetry/android/instrumentation/myfeature/
      MyFeatureInstrumentation.kt (The AndroidInstrumentation implementation)
    src/test/kotlin/io/opentelemetry/android/instrumentation/myfeature/
      MyFeatureInstrumentationTest.kt
```

Historical modules may use `src/main/java` for Kotlin files. New Kotlin code can use
`src/main/kotlin`.

The top-level `settings.gradle.kts` automatically includes modules under
`instrumentation/` when a `build.gradle.kts` file is present, so a new instrumentation
module does not usually need a settings change.

### Gradle Setup

Use the Android library and publishing convention plugins:

```kotlin
plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android my feature instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.myfeature"
}

dependencies {
    implementation(project(":instrumentation:android-instrumentation")) // This is where the AndroidInstrumentation API is located.
}
```

Add `consumer-rules.pro` only when the instrumentation depends on reflection or class
names being retained.

### Implement `AndroidInstrumentation`

Each instrumentation has one entry point that implements `AndroidInstrumentation` and is
registered with `@AutoService(AndroidInstrumentation::class)`.

```kotlin
@AutoService(AndroidInstrumentation::class)
class MyFeatureInstrumentation : AndroidInstrumentation {
    override val name: String = "my-feature"

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        val logger =
            openTelemetryRum.openTelemetry.logsBridge
                .loggerBuilder("io.opentelemetry.android.instrumentation.my-feature")
                .build()

        // Register callbacks, listeners, or services here.
    }

    override fun uninstall(context: Context, openTelemetryRum: OpenTelemetryRum) {
        // Unregister anything installed above.
    }
}
```

The `otel.android-library-conventions` plugin already applies KSP and the AutoService
processor so that your instrumentation can get automatically discovered at runtime. You only need the annotation.

The `name` is the instrumentation's identifier. It's currently used to allow suppressing instrumentations by name at runtime via the
Android Agent DSL. Keep it short, unique, and stable.

### Installation Rules

- `install()` should attach the instrumentation and return quickly. It receives the Android
application `Context` and the initialized `OpenTelemetryRum`, so use those instead of
global state where possible.
- Store registered callbacks or listeners in fields so `uninstall()` can remove them.
- Avoid expensive work on the main thread. If an instrumentation needs background work,
make it manage its own background thread and ensure it can be shut down from `uninstall()`.

### Configuration

Expose configuration only when users need it. This is done by exposing mutable
properties or setter methods on the instrumentation instance, and those values are read
during `install()`.

Configuration must happen before `OpenTelemetryRum` is built. The loader discovers an
instrumentation instance from the classpath, and the builder installs it during
RUM initialization. So the configuration is read once per RUM initialization.

### Telemetry

Follow OpenTelemetry semantic conventions where they exist. Keep instrumentation scope
names stable and specific, for example
`io.opentelemetry.android.instrumentation.<feature>`.

Choose the signal based on what happened:

- Spans for operations with duration, parent context, status, and errors.
- Log events for point-in-time events such as lifecycle or click events.
- Metrics for measurements intended to be aggregated over time.

Use data extractors from upstream OpenTelemetry instrumentation APIs when they fit.

## ByteBuddy Instrumentation Layout

ByteBuddy instrumentations are split into submodules:

```text
instrumentation/
  my-library/
    README.md
    library/
      build.gradle.kts
      src/main/kotlin/io/opentelemetry/instrumentation/library/mylibrary/
        MyLibraryInstrumentation.kt (The AndroidInstrumentation implementation)
    agent/
      build.gradle.kts
      src/main/kotlin/io/opentelemetry/instrumentation/agent/mylibrary/
        MyLibraryPlugin.kt (ByteBuddy plugin)
      src/main/java/io/opentelemetry/instrumentation/agent/mylibrary/
        MyLibraryAdvice.java (ByteBuddy advice)
    testing/
      build.gradle.kts
      src/androidTest/kotlin/io/opentelemetry/instrumentation/library/mylibrary/
        InstrumentationTest.kt
```

The existing `okhttp3`, `okhttp3-websocket`, `httpurlconnection`, and `android-log`
instrumentations use this shape.

### Library Submodule

The `library` submodule owns the normal runtime instrumentation entry point and the code
that creates telemetry. It is the module users add with `implementation(...)`.

```kotlin
plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry MyLibrary library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.mylibrary.library"
}

dependencies {
    implementation(project(":instrumentation:android-instrumentation")) // Contains the AndroidInstrumentation API
}
```

Similarly to the "Regular instrumentation", the `AndroidInstrumentation` implementation still lives here and still uses
`@AutoService(AndroidInstrumentation::class)`. Its `install()` method usually configures
shared singletons that the woven code will call later.

Keep the telemetry implementation in `library`, not `agent`, as the `agent` module won't be part of the host app's runtime dependencies (the agent is only used at compile time to do bytecode weaving).

### Agent Submodule

The `agent` submodule owns ByteBuddy plugins and advice. It is the module users add with
`byteBuddy(...)` and it's only used during compilation (it's not added as a runtime dependency of the target app).

```kotlin
plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for MyLibrary on Android"

android {
    namespace = "io.opentelemetry.android.mylibrary.agent"
}

dependencies {
    implementation(project(":instrumentation:my-library:library")) // You should add your instrumentation's library here to being able to reference runtime code.
    implementation(libs.byteBuddy) // ByteBuddy's plugin APIs live here
}
```

A plugin implements `net.bytebuddy.build.Plugin`, matches the classes to transform, and
applies advice or member substitutions:

```kotlin
internal class MyLibraryPlugin : Plugin {
    override fun matches(target: TypeDescription): Boolean =
        target.typeName == "com.example.MyLibraryClient"

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator,
    ): DynamicType.Builder<*> =
        builder.visit(
            Advice.to(MyLibraryAdvice::class.java)
                .on(ElementMatchers.named("execute")),
        )

    override fun close() {
        // No operation.
    }
}
```

Advice should do the minimum necessary to bridge from instrumented code into the
`library` module. For example, OkHttp advice adds interceptors to an `OkHttpClient`
builder, while HttpURLConnection advice redirects platform calls to replacement methods.

### Testing Submodule

ByteBuddy weaving must be verified with an Android test app. Unit tests and Robolectric do
not exercise Android build-time weaving.

```kotlin
plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android {
    namespace = "io.opentelemetry.android.mylibrary.test"
}

dependencies {
    byteBuddy(project(":instrumentation:my-library:agent"))
    implementation(project(":instrumentation:my-library:library"))
}
```

Write tests under `src/androidTest`. The test should use the target library as an app
developer would use it, without calling agent internals. `OpenTelemetryRumRule` from
`test-common` initializes RUM with in-memory exporters and is useful for asserting spans
or logs emitted by woven code.

Add regular unit tests in the `library` module for pure telemetry helpers, extractors,
and configuration behavior.

## Documentation

Every instrumentation should have a README in its module directory. Match the existing
style:

- A short title and `Status: development`.
- A description of what API or behavior is instrumented.
- A `Telemetry` section listing emitted spans, logs, metrics, names, and important
  attributes.
- Installation instructions.
- Configuration instructions, including the requirement to configure before
  `OpenTelemetryRum` initialization.
- ByteBuddy plugin and `byteBuddy(...)` dependency instructions when applicable.

## Validation Checklist

Before opening a PR:

- Confirm the instrumentation has a stable `name` and is registered with `@AutoService`.
- Confirm `install()` registers all required callbacks and `uninstall()` removes owned
  callbacks, listeners, or workers.
- Add unit tests for regular instrumentation behavior.
- Add an Android `testing` app for ByteBuddy instrumentation.
