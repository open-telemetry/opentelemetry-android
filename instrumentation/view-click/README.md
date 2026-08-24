
# View Click Instrumentation

Status: development

This instrumentation has the ability to generate events when the user
performs click actions. It detects both single taps and double taps across
touch and other pointer input types.

When an Activity becomes active, the instrumentation begins tracking
its window by registering a callback that receives events.

This instrumentation is not currently enabled by default.

## Telemetry

Data produced by this instrumentation will have an instrumentation scope
name of `io.opentelemetry.android.instrumentation.view.click`.
This instrumentation produces the following telemetry:

### Clicks

* Type: Event
* Name: `app.screen.click`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on the screen.
* Common attributes:
  * `app.screen.coordinate.x`
  * `app.screen.coordinate.y`
  * `hw.pointer.clicks` (`1` for a single tap, `2` for a double tap)
  * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
  * `hw.pointer.button` for supported mouse and stylus button interactions
* See the [semantic convention definition](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/app/app-events.md#event-appscreenclick)
  for more details.

* Type: Event
* Name: `app.widget.click`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`. Jetpack Compose views are not currently supported.
* Common attributes:
  * `app.widget.id`
  * `app.widget.name`
  * `app.screen.coordinate.x`
  * `app.screen.coordinate.y`
  * `hw.pointer.clicks` (`1` for a single tap, `2` for a double tap)
  * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
  * `hw.pointer.button` for supported mouse and stylus button interactions
* See the [semantic convention definition](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/app/app-events.md#event-appwidgetclick)
  for more details.

* Type: Event
* Name: `app.screen.longpress`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `app.screen.id`
    * `app.screen.name`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
* See the [semantic convention definition](https://github.com/open-telemetry/opentelemetry-android/blob/1d7cd77538bd0e83c8efb1d80d6edbf8c0f365f5/semconv/model/android/events.yaml#L11)
  for more details.

* Type: Event
* Name: `app.widget.longpress`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `app.widget.id`
    * `app.wideget.name`
    * `app.screen.id`
    * `app.screen.name`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
* See the [semantic convention definition](https://github.com/open-telemetry/opentelemetry-android/blob/1d7cd77538bd0e83c8efb1d80d6edbf8c0f365f5/semconv/model/android/events.yaml#L30)
  for more details.

* Type: Event
* Name: `app.screen.scroll`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `app.screen.id`
    * `app.screen.name`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.distance.x`
    * `hw.pointer.distance.y`
* See the [semantic convention definition](https://github.com/open-telemetry/opentelemetry-android/blob/1d7cd77538bd0e83c8efb1d80d6edbf8c0f365f5/semconv/model/android/events.yaml#L53)
  for more details.

* Type: Event
* Name: `app.widget.scroll`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `app.widget.id`
    * `app.wideget.name`
    * `app.screen.id`
    * `app.screen.name`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.distance.x`
    * `hw.pointer.distance.y`
* See the [semantic convention definition](https://github.com/open-telemetry/opentelemetry-android/blob/1d7cd77538bd0e83c8efb1d80d6edbf8c0f365f5/semconv/model/android/events.yaml#L76)
  for more details.

* Type: Event
* Name: `app.screen.fling`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `app.screen.id`
    * `app.screen.name`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.velocity.x`
    * `hw.pointer.velocity.y`
* See the [semantic convention definition](https://github.com/open-telemetry/opentelemetry-android/blob/1d7cd77538bd0e83c8efb1d80d6edbf8c0f365f5/semconv/model/android/events.yaml#L103)
  for more details.

* Type: Event
* Name: `app.widget.fling`
* Description: This event is emitted when the user performs a confirmed single tap or double tap on a clickable Android `View`.
* Common attributes:
    * `app.screen.coordinate.x`
    * `app.screen.coordinate.y`
    * `app.widget.id`
    * `app.wideget.name`
    * `app.screen.id`
    * `app.screen.name`
    * `hw.pointer.type` (`finger`, `mouse`, `stylus`, `eraser`, or `unknown`)
    * `hw.pointer.button` for supported mouse and stylus button interactions
    * `hw.pointer.velocity.x`
    * `hw.pointer.velocity.y`
* See the [semantic convention definition](https://github.com/open-telemetry/opentelemetry-android/blob/1d7cd77538bd0e83c8efb1d80d6edbf8c0f365f5/semconv/model/android/events.yaml#L126)
  for more details.


## Installation

### Adding dependencies

```kotlin
implementation("io.opentelemetry.android.instrumentation:view-click:1.6.0-alpha")
```
