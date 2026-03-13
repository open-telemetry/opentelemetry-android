
# Slow Rendering Instrumentation

Status: development

The OpenTelemetry slow rendering instrumentation for Android will detect when
the application user interface is slow or frozen.
[See the Android documentation for a discussion of UI "jank"](https://developer.android.com/studio/profile/jank-detection).

The instrumentation operates by periodically polling for frame metrics, by default
every 1 second.

## Telemetry

This instrumentation produces the following telemetry, with an instrumentation
scope of `app.jank`.

### Slow Renders (Event)

Generated when rendering takes more than 16ms within a polling period.

* Type: Event
* Event Name: `app.jank`
* Description: This event is emitted when frame metrics contain at least
  one render duration longer than 16ms (the slow rendering threshold).
* Attributes:
  * `app.jank.frame_count` - the number of frames that exceeded the threshold
  * `app.jank.period` - the polling period duration in seconds during which the frames were detected
  * `app.jank.threshold` - the threshold in seconds above which a frame is considered slow (e.g. `0.016`)

### Frozen Renders (Event)

Generated when rendering takes more than 700ms within a polling period.

* Type: Event
* Event Name: `app.jank`
* Description: This event is emitted when frame metrics contain at least
  one render duration longer than 700ms (the frozen rendering threshold).
* Attributes:
  * `app.jank.frame_count` - the number of frames that exceeded the threshold
  * `app.jank.period` - the polling period duration in seconds during which the frames were detected
  * `app.jank.threshold` - the threshold in seconds above which a frame is considered frozen (e.g. `0.7`)

### Deprecated: Zero-Duration Spans

> **Deprecated.** Zero-duration spans are no longer emitted by default. They can be re-enabled
> via `enableDeprecatedZeroDurationSpan()` for backwards compatibility, but this is discouraged.
> Use the `app.jank` events above instead.

When enabled via `enableDeprecatedZeroDurationSpan()`, the instrumentation additionally produces
spans with an instrumentation scope of `io.opentelemetry.slow-rendering`.

#### Slow Renders (Span)

* Type: Span (zero duration)
* Name: `slowRenders`
* Attributes:
  * `count` - the number of slow renders
  * `activity.name` - the name of the activity for which the slow render was detected

#### Frozen Renders (Span)

* Type: Span (zero duration)
* Name: `frozenRenders`
* Attributes:
  * `count` - the number of frozen renders
  * `activity.name` - the name of the activity for which the frozen render was detected

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:slowrendering:1.2.0-alpha")
```
