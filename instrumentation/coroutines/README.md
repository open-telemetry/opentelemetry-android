# Kotlin coroutines

Status: development

Automatically propagates the current OpenTelemetry `Context` into Kotlin coroutines started with
`CoroutineScope.launch`. It does **not** create coroutine spans or emit any other telemetry.

## What this does in practice

Given:

```kotlin
val span = tracer.spanBuilder("work").startSpan()
span.makeCurrent().use {
    scope.launch {
        val inner = tracer.spanBuilder("coroutine-work").startSpan()
        // ...
        inner.end()
    }
}
span.end()
```

**Without** this instrumentation, `coroutine-work` does not inherit the active context and is recorded
as a separate root span:

```
work
coroutine-work
```

**With** this instrumentation, the active context is carried into the coroutine automatically, and
`coroutine-work` is recorded as a child of `work`:

```
work
└── coroutine-work
```

## Supported API

- `CoroutineScope.launch` (both the default-parameter form and the explicit form)

## Transformation scope

Only classes local to the module that applies the ByteBuddy plugin are transformed. External
library and dependency classes are left unchanged.

## Explicit exclusions

The following are out of scope for this instrumentation:

- External dependencies and sibling library modules consumed as dependencies
- `async`, `withContext`, `runBlocking`, Flow, and all other coroutine builders

## Installation

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

Propagation is one-directional: context flows from the calling thread into the coroutine at launch
time. Work dispatched from within a coroutine to a plain Java `Executor` or thread starts with a
fresh context, as it would without this instrumentation.

## Suppression

This instrumentation can be suppressed by its stable name `"coroutines"` using the standard
OpenTelemetry Android suppression mechanism.
