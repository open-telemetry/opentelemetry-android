
# Slow Rendering Instrumentation

Status: development

The OpenTelemetry slow rendering instrumentation for Android will detect when
the application user interface is slow or frozen.
[See the Android documentation for a discussion of UI "jank"](https://developer.android.com/studio/profile/jank-detection).

The instrumentation operates by periodically polling for frame metrics, by default
every 1 second.

## Telemetry

This instrumentation produces the following telemetry, with an instrumentation
scope of `io.opentelemetry.slow-rendering`.

### Slow Renders

Generated when rendering takes more than 16ms.

* Type: Span (zero duration)
* Name: `slowRenders`
* Description: This event is emitted when frame metrics contain at least
  one render duration longer than 16ms.
* Attributes:
  * `count` - the number of slow renders
  * `activity.name` - the name of the activity for which the slow render was detected

### Frozen Renders

Generated when rendering takes more than 700ms.

* Type: Span (zero duration)
* Name: `frozenRenders`
* Description: This event is emitted when frame metrics contain at least
  one render duration longer than 700ms.
* Attributes:
    * `count` - the number of slow renders
    * `activity.name` - the name of the activity for which the slow render was detected

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:slowrendering:1.0.0-rc.1-alpha")
```
