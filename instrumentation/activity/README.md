
# Activity Instrumentation

Status: experimental

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
* Name: {`Created` | `Resumed` | `Paused` | `Stopped` | `Destroyed`} (depends on the activity state transition)
* Description: As the activity transitions between states, a span will be created to represent the
  lifecycle of that state. Events are added for subsequent minor state changes.
* SpanEvents: {
  `activityPreCreated` | `activityCreated` | `activityPostCreated` |
  `activityPreResumed` | `activityResumed` | `activityPostResumed` |
  `activityPrePaused` | `activityPaused` | `activityPostPaused` |
  `activityPreStopped` | `activityStopped` | `activityPostStopped` |
  `activityPreDestroyed` | `activityDestroyed` | `activityPostDestroyed` }
* Attributes:
  * `activity.name`:  <name of activity>
  * `screen.name`:  <name of screen>
