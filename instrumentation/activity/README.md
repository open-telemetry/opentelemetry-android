
# Activity Instrumentation

Status: development

The activity instrumentation helps to track the state of your application's
Activity and the lifecycle. This instrumentation also currently measures
and reports some application startup telemetry.

## Telemetry

This instrumentation produces the following telemetry:

### Application Start

* Type: Span
* Name: `AppStart`
* Description: This span is created and started when the Activity instrumentation is
  installed. It is ended when the initial activity reaches PostPaused, PostStopped, or PostDestroyed.
* Attributes:
  * `start.type`: { `cold` | `hot` | `warm` }

### Activity state change

* Type: Span
* Name: {`Created` | `Resumed` | `Paused` | `Stopped` | `Destroyed` | `Restarted` |} (depends on the activity state transition)
* Description: As the activity transitions between states, a span will be created to represent the
  lifecycle of that state. Events are added for subsequent minor state changes.
* SpanEvents: {
  `activityPreCreated` | `activityCreated` | `activityPostCreated` |
  `activityPreStarted` | `activityStarted` | `activityPostStarted` |
  `activityPreResumed` | `activityResumed` | `activityPostResumed` |
  `activityPrePaused` | `activityPaused` | `activityPostPaused` |
  `activityPreStopped` | `activityStopped` | `activityPostStopped` |
  `activityPreDestroyed` | `activityDestroyed` | `activityPostDestroyed` }
* Attributes:
  * `activity.name`:  name of activity
  * `screen.name`:  name of screen
  * `last.screen.name`:  name of screen, only when span contains the `activityPostResumed` event.

## Installation

This instrumentation comes with the [android agent](../../android-agent) out of the box, so
if you depend on it, you don't need to do anything else to install this instrumentation.
However, if you don't use the agent but instead depend on [core](../../core) directly, you can
manually install this instrumentation by following the steps below.

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:activity:1.0.0-rc.1-alpha")
```
