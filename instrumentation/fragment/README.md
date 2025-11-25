# Fragment Instrumentation

Status: development

The fragment instrumentation helps to track the state of your application's Fragment lifecycle.

## Telemetry

This instrumentation produces the following telemetry:

### Fragment state change

* Type: Span
* Name: {`Created` | `Restored` | `Resumed` | `Paused` | `Stopped` | `Destroyed` | `ViewDestroyed` | `Detached` |} (depends on the activity state transition)
* Description: As the activity transitions between states, a span will be created to represent the
  lifecycle of that state. Events are added for subsequent minor state changes.
* SpanEvents: {
  `fragmentPreAttached` | `fragmentAttached` | `fragmentPreCreated` | `fragmentCreated` | `fragmentViewCreated`
  `fragmentStarted` | `fragmentResumed` | `fragmentPaused` | `fragmentStopped` |
  `fragmentViewDestroyed` | `fragmentDestroyed` | `fragmentDetached` }
* Attributes:
    * `fragment.name`:  name of fragment
    * `screen.name`:  name of screen
    * `last.screen.name`:  name of screen, when span contains the `fragmentResumed` event.

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:fragment:1.0.0-rc.1-alpha")
```
