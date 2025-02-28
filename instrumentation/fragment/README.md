# Fragment Instrumentation

Status: experimental

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
  `fragmentViewDestroyed` | `fragmentDestroyed` | `fragmentDetached`
* Attributes:
    * `fragmentName`:  <name of fragment>
    * `screen.name`:  <name of screen>
    * `last.screen.name`:  <name of screen>, when span contains the `fragmentResumed` event.
* Parent:
