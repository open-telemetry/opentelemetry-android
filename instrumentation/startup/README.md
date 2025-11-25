# Startup Instrumentation

Status: development

The startup instrumentation provides initialization log events that describe the steps taken during
RUM initialization.

## Telemetry

This instrumentation produces the following telemetry:

### SDK Initialization

* Type: Event
* Name: { `rum.sdk.init.started` | `rum.sdk.init.net.provider` | `rum.sdk.init.net.monitor` | `rum.sdk.init.anr_monitor` | `rum.sdk.init.jank_monitor` | `rum.sdk.init.crash.reporter` | `rum.sdk.init.span.exporter` }
* Description: These events indicate the progress of various RUM SDK initialization components.
* Attributes:
    * `span.exporter`: *(Only for `rum.sdk.init.span.exporter`)* — Name of the configured span exporter.

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:startup:1.0.0-rc.1-alpha")
```
