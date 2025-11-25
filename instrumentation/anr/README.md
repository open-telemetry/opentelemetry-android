
# ANR (Application Not Responding) Instrumentation

Status: development

The ANR (Application Not Responding) instrumentation helps to detect
when an application becomes unresponsive. This instrumentation functions
by polling the UI thread once every second, verifying that the main UI
thread is still active. If 5 consecutive checks fail, then an ANR condition
has occurred, and telemetry will be generated.

The ANR instrumentation is only active when the application is in the
foreground.

## Telemetry

This instrumentation produces the following telemetry:

### ANR

* Type: Event
* Name: `ANR`
* Description: This log event is created when this instrumentation detects an ANR.
* Status: `ERROR` (always)
* Attributes:
  * `exception.stacktrace`: A string representation of the call stack of the main thread at the time of the ANR.
    ([see semconv here](https://github.com/open-telemetry/semantic-conventions/blob/0b3babde7ff9f74b03a1a49adcdb319354d47d85/docs/attributes-registry/exception.md#exception-stacktrace))

Note: This instrumentation supports additional user-configurable `AttributeExtractors` that
may set additional attributes from the given `StackTraceElement[]`.

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:anr:1.0.0-rc.1-alpha")
```
