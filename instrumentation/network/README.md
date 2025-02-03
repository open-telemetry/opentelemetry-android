
# Network Change Instrumentation

Status: experimental

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

* Type: Span
* Name: `network.change`
* Description: This zero-duration span is started and ended when a network change is detected.
* Attributes:
    * `network.status`: One of `lost` or `available`.
    * `network.connection.type` (semconv) one of `cell`, `wifi`, `wired`, `unavailable`, `unknown`, `vpn`.

Note: This instrumentation supports additional user-configurable `AttributeExtractors` that
may set additional attributes when given a `CurrentNetwork` instance.
