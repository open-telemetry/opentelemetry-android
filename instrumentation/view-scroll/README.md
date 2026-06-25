# View Scroll Instrumentation

Status: development

This instrumentation has the ability to generate events when the user
moves a finger across the screen. It detects scrolls and flings across
touch and other pointer input types.

When an Activity becomes active, the instrumentation begins tracking
its window by registering a callback that receives events.

This instrumentation is not currently enabled by default.

## Telemetry

Data produced by this instrumentation will have an instrumentation scope
name of `io.opentelemetry.android.instrumentation.view.scroll`.
This instrumentation produces the following telemetry:

### Scroll

* Type: Event
* Name: `app.screen.scroll`
* Description: This event is emitted when the user moves their pointer across the screen without letting go.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.distance.x`
    * `hw.pointer.distance.y`

* Type: Event
* Name: `app.widget.scroll`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`. Jetpack Compose views are not currently supported.
* Common attributes:
    * `app.widget.id`
    * `app.widget.name`
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.distance.x`
    * `hw.pointer.distance.y`

### Fling

* Type: Event
* Name: `app.screen.fling`
* Description: This event is emitted when the user moves their finger across the screen and 
terminates the gesture by letting go of the pointer.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.velocity.x`
    * `hw.pointer.velocity.y`


* Type: Event
* Name: `app.widget.fling`
* Description: This event is emitted when the user moves their finger across the screen and
  terminates the gesture by letting go of the pointer.
* Common attributes:
    * `app.widget.id`
    * `app.widget.name`
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.velocity.x`
    * `hw.pointer.velocity.y`