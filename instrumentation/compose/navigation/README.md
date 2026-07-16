
# Compose Navigation Instrumentation

Status: development

## Navigation version
Targets `androidx.navigation:navigation-compose` (Jetpack Compose Navigation).

This instrumentation has the ability to generate an event whenever the current
navigation destination of a `NavController` changes.

It is a manual instrumentation: because Compose Navigation has no global callback,
you attach it to the controller you already hold and pass in your
`OpenTelemetryRum` instance. Per-`NavController` hookup is always explicit.

This instrumentation is not currently enabled by default.

## Telemetry

Data produced by this instrumentation will have an instrumentation scope
name of `io.opentelemetry.android.instrumentation.compose.navigation`.
This instrumentation produces the following telemetry:

### Screen views

* Type: Event
* Name: `app.screen.view` (provisional, pending mobile semantic convention agreement)
* Description: This event is emitted when the current navigation destination changes.
* Attributes:
  * `app.screen.name`: the screen name derived from the destination. By default, this
    is the route *pattern* (for example `user/{id}`, not the filled-in arguments) to
    avoid leaking PII.

## Installation

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:compose-navigation:1.6.0-alpha")
```

### Instrumenting a NavController

Pass your `OpenTelemetryRum` instance to either entry point.

Swap `rememberNavController()` for the drop-in factory:

```kotlin
val navController = rememberObservedNavController(rum = myRum)
NavHost(navController, startDestination = "home") { /* ... */ }
```

Or attach the extension to an existing controller. This works on any `NavController`
you already hold — including nested/child controllers, not just the host:

```kotlin
val navController = rememberNavController().withOpenTelemetry(rum = myRum)
```

Override how a destination maps to a screen name if the default route pattern is
not what you want:

```kotlin
val navController =
    rememberNavController().withOpenTelemetry(
        rum = myRum,
        screenName = { destination, _ -> destination.route ?: "unknown" },
    )
```
