# Screen Orientation Instrumentation

Status: development

The OpenTelemetry screen orientation instrumentation for Android will detect when the screen
orientation changes.

## Telemetry

This instrumentation produces the following telemetry, with an instrumentation
scope of `io.opentelemetry.screen_orientation`.

### Screen Orientation

* Type: Event
* Name: `device.screen_orientation`
* Description: An event representing a change in screen orientation.
* Attributes:
    * `screen.orientation` - Represents the changed screen orientation

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
