# Power Save Mode Instrumentation

Status: development

The OpenTelemetry power save mode instrumentation for Android detects when the device's power
save (battery saver) mode is turned on or off.

It listens for [`PowerManager.ACTION_POWER_SAVE_MODE_CHANGED`](https://developer.android.com/reference/android/os/PowerManager#ACTION_POWER_SAVE_MODE_CHANGED)
broadcasts (no polling) and emits one event per change, reading the current state from
[`PowerManager.isPowerSaveMode`](https://developer.android.com/reference/android/os/PowerManager#isPowerSaveMode()).

This instrumentation is not currently enabled by default.

## Telemetry

This instrumentation produces the following telemetry, with an instrumentation scope of
`io.opentelemetry.power_save_mode`.

### Power Save Mode

* Type: Event
* Name: `device.power_save_mode.change`
* Description: An event representing a change in the device's power save mode.
* Attributes:
    * `android.power_save_mode.enabled` - A boolean value that is `true` when power save mode is
    enabled and `false` when it is disabled.

## Installation

This instrumentation does not come with the [android agent](../../android-agent) out of the box,
so you need to manually install it as described below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:power-save-mode:LATEST_VERSION")
```
