# Agent API Kotlin extensions

Optional module that exposes the
[opentelemetry-kotlin](https://github.com/open-telemetry/opentelemetry-kotlin) API from an existing
`OpenTelemetryRum` instance.

> **Not stable.** This module may change its API or
> be removed in any release, with no deprecation cycle. opentelemetry-kotlin is itself pre-1.0.

## Installation

```kotlin
dependencies {
    api(platform("io.opentelemetry.android:opentelemetry-android-bom:<version>-alpha"))
    implementation("io.opentelemetry.android:agent-api-ktx") // Version is resolved through the BOM
}
```

## Usage

```kotlin
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.openTelemetryKotlin
import io.opentelemetry.kotlin.ExperimentalApi

@OptIn(ExperimentalApi::class)
fun example(rum: OpenTelemetryRum) {
    val otel = rum.openTelemetryKotlin

    val logger = otel.loggerProvider.getLogger("my_logger")
    logger.log("Hello, World!")

    val tracer = otel.tracerProvider.getTracer("my_tracer")
    tracer.startSpan("my_span").end()
}
```

`@OptIn` is required for most calls as opentelemetry-kotlin APIs are mostly marked `@ExperimentalApi`.
See the [upstream getting started guide](https://opentelemetry.io/docs/languages/kotlin/getting-started/)
for an alternative approach that opts in globally through Kotlin's compiler options.

## What you get

`openTelemetryKotlin` returns the *compat* implementation of opentelemetry-kotlin: a Kotlin API that
delegates to the OpenTelemetry Java SDK the agent already configures. Spans,
logs and metrics recorded through it flow through the same processors and exporters as telemetry
recorded through `OpenTelemetryRum.openTelemetry`, and the two APIs can be used side by side.

The same instance is returned on every access.
