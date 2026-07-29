# coroutines

Status: development

Automatically propagates the current OpenTelemetry `Context` into Kotlin coroutines started with
`CoroutineScope.launch`. It does **not** create coroutine spans or emit any other telemetry.

## Supported API

- `CoroutineScope.launch` (both the default-parameter form and the explicit form)

## Transformation scope

Only classes local to the module that applies the ByteBuddy plugin are transformed. External
library and dependency classes are left unchanged.

## Explicit exclusions

The following are out of scope for this instrumentation:

- External dependencies and sibling library modules consumed as dependencies
- `async`, `withContext`, `runBlocking`, Flow, and all other coroutine builders

## Setup

Add the ByteBuddy Gradle plugin to your application module:

```kotlin
plugins {
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

dependencies {
    implementation("io.opentelemetry.android.instrumentation:coroutines-library:<version>")
    byteBuddy("io.opentelemetry.android.instrumentation:coroutines-agent:<version>")
}
```

## Context precedence

If `CoroutineScope.launch` is called with an explicit, non-root OpenTelemetry coroutine context
element already in the supplied `CoroutineContext`, that user-supplied context takes precedence and
automatic capture is skipped.

## Suppression

This instrumentation can be suppressed by its stable name `"coroutines"` using the standard
OpenTelemetry Android suppression mechanism.
