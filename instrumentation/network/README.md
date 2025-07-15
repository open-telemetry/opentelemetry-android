
# Network Change Instrumentation

Status: development

Android applications are typically deployed on mobile devices. These mobile devices
have the ability to move between networks, and sometimes a network change can
have an impact on application performance characteristics. This instrumentation
will generate telemetry when the network changes, as detected
via [ConnectivityManager.NetworkCallback](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback).

This instrumentation only generates telemetry when the application is in the foreground,
not when the application is backgrounded.

## Telemetry

This instrumentation produces the following telemetry:

### Network Change

* Type: Event
* Name: `network.change`
* Description: This event is emitted when a network change is detected.
* Attributes:
    * `network.status`: One of `lost` or `available`.
    * `network.connection.type` (semconv) one of `cell`, `wifi`, `wired`, `unavailable`, `unknown`, `vpn`.

Note: This instrumentation supports additional user-configurable `AttributeExtractors` that
may set additional attributes when given a `CurrentNetwork` instance.

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:network:LATEST_VERSION") // <1>
```

1. You can find the latest version [here](https://central.sonatype.com/artifact/io.opentelemetry.android.instrumentation/network).
