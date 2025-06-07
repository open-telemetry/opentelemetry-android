
# Compose Instrumentation

Status: development

## Compose version
`1.3.0+`

This instrumentation has the ability to generate events when the user
performs click actions. A click is not differentiated from touch or other
input pointer events.

When an Activity becomes active, the instrumentation begins tracking
its window by registering a callback that receives events.

This instrumentation is not currently enabled by default.

## Telemetry

Data produced by this instrumentation will have an instrumentation scope
name of `io.opentelemetry.android.instrumentation.compose`.
This instrumentation produces the following telemetry:

### Clicks

* Type: Event
* Name: `app.screen.click`
* Description: This event is emitted when the user taps or clicks on the screen.
* See the [semantic convention definition](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/app/app.md#event-appscreenclick)
  for more details.

* Type: Event
* Name: `event.app.widget.click`
* Description: This event is emitted when the user taps on a composable that is clickable.
* See the [semantic convention definition](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/app/app.md#event-appwidgetclick)
  for more details.
