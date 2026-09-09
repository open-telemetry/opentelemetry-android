
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

This instrumentation produces the following telemetry:

### Navigation

* Type: Event
* Name: `app.navigation.complete`
* Description: An event that fires on every completed navigation of an instrumented
  `NavController`, plus on listener attach (see below).
* Attributes:

| Attribute | Type | Description | Values | Requirement Level |
|---|---|---|---|---|
| `app.navigation.destination.name` | string | The name of the navigation destination reached by the completed navigation. | `home`; `user/{id}` | Required |

The destination name defaults to the route *pattern*, for example `user/{id}` —
not the filled-in arguments — to avoid leaking PII. Override it with the
`screenName` parameter shown below.

The event maps 1:1 to `NavController.OnDestinationChangedListener` callbacks, so it
also fires:

* **on attach** — registering replays the current destination, so the screen visible
  at attach time is recorded. A configuration change re-attaches and replays again
  without a navigation. The replay supplies no `arguments`, even for a parameterised
  start destination.
* **on argument-only changes** — `user/1` → `user/2` is a real navigation, but the
  default resolver names both `user/{id}`. Read `arguments` in `screenName` to tell
  them apart, subject to the attach caveat above.

Using these Compose screen names for screen attribution on unrelated telemetry
is tracked separately in
[#1909](https://github.com/open-telemetry/opentelemetry-android/issues/1909).

## Installation

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:compose-navigation:1.7.0-alpha")
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
