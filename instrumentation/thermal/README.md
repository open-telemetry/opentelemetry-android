# Thermal Status Instrumentation

Status: development

The OpenTelemetry thermal status instrumentation for Android detects when the device's
thermal throttling status changes (for example, when the device starts to overheat).

It listens for changes via [`PowerManager.addThermalStatusListener`](https://developer.android.com/reference/android/os/PowerManager#addThermalStatusListener(java.util.concurrent.Executor,%20android.os.PowerManager.OnThermalStatusChangedListener))
(no polling) and emits one event per status change.

The thermal status APIs are only available on API level 29 (Android Q) and higher, so this
instrumentation no-ops on older devices.

This instrumentation is not currently enabled by default.

## Telemetry

This instrumentation produces the following telemetry, with an instrumentation
scope of `io.opentelemetry.thermal`.

### Thermal State

* Type: Event
* Name: `device.thermal_status.change`
* Description: An event representing a change in the device's thermal throttling status.
* Attributes:
    * `android.thermal.state` - A string value that contains the new thermal state.
    Values are `none`, `light`, `moderate`, `severe`, `critical`, `emergency`,
    `shutdown`, or `unknown`.

## Installation

This instrumentation does not come with the [android agent](../../android-agent) out of the
box, so you need to manually install it as described below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:thermal:LATEST_VERSION")
```
