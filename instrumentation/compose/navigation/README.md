
# Compose Navigation Instrumentation

Status: development

## Navigation version
Targets `androidx.navigation:navigation-compose` (Jetpack Compose Navigation).

This instrumentation observes completed navigations: whenever the current
destination of a `NavController` changes, it resolves a screen name for the
new destination. Note that a destination change means a navigation has
completed — it does not guarantee that any meaningful UI has rendered.

It is a manual instrumentation: because Compose Navigation has no global callback,
you attach it to the controller you already hold and pass in your
`OpenTelemetryRum` instance. Per-`NavController` hookup is always explicit.

This instrumentation is not currently enabled by default.

## Telemetry

This instrumentation does not emit any telemetry yet. Destination changes are
resolved to screen names (by default the route *pattern*, for example
`user/{id}` — not the filled-in arguments — to avoid leaking PII). How they are
reported — as an event and/or as screen attribution on other telemetry — is
tracked in
[#1909](https://github.com/open-telemetry/opentelemetry-android/issues/1909),
pending the mobile semantic-conventions discussion on modelling navigation.

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
