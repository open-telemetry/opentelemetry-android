
# OpenTelemetry Android Demo App

This is an app built to demonstrate how to configure and use the OpenTelemetry Android agent
to observe app and user behavior.

This is very much a work in progress. See the `OtelDemoApplication.kt` for 
a quick and dirty example of how to get the agent initialized.

## Features

The OpenTelemetry Android Demo App currently supports the following features:

* Android Activity Lifecycle Monitoring
  - Automatically captures spans for key lifecycle events:
    - Created: Includes `onCreate`, `onStart`, `onResume`,
    - Paused: Includes `onPause`,
    - Stopped: Includes `onStop`,
    - Destroyed: Includes `onDestroy`.
  - This covers the entire Activity lifecycle, providing detailed insights into each phase.

* Fragment Lifecycle Monitoring
  - Automatically captures spans for key lifecycle events:
    - Attached: `onAttach` (fragment attached to context),
    - Created: `onCreate`,
    - View Created: `onViewCreated` (UI created),
    - Started: `onStart`,
    - Resumed: `onResume` (fragment active),
    - Paused: `onPause`,
    - Stopped: `onStop`,
    - View Destroyed: `onDestroyView` (UI removed),
    - Destroyed: `onDestroy`,
    - Detached: `onDetach` (fragment disconnected).
  - Provides detailed insights into each lifecycle phase.
  - Can be observed in the "About OpenTelemetry Android" activity, entered via "Learn more" on the main screen.

* Crash Reporting  
  - Automatically detects and reports a crash of the application.
  - In order to crash the demo app, try to add to cart exactly 10 National Park Foundation Explorascopes (first product on the list after clicking "Go shopping") and click "Yes, I'm sure." on the alert pop-up. This will cause a multi-threaded crash of the app.
  - Note: The crash is reported as an event and isn't visible in the Jaeger UI, only in the collector output.

* ANR Detection
  - Automatically detects and reports ANRs in the app.
  - ANR events are captured as spans with detailed stack traces, providing insights into the exact operations that caused the ANR.
  - The span includes key attributes such as `screen.name`, `session.id`, and network information to assist in diagnosing the issue.
  - In order to crash the demo app, try to add to cart exactly 9 National Park Foundation Explorascopes (first product on the product list) and click "Yes, I'm sure." on the alert pop-up.


* Slow Render Detection
  - Automatically detects instances of slow rendering within the app.
  - Slow render events are captured as spans, providing information on when and where rendering delays occurred.
  - The span includes attributes such as `activity.name`, `screen.name`, `count`, and network details to help diagnose performance issues.
  - To trigger a slowly rendering animation in the demo app, add any quantity of The Comet Book (the last product on the product list) to the cart. Note that the number of `slow-render` spans and their respective `count` attributes may vary between runs or across different machines.

* Manual Instrumentation
  - Provides access to the OpenTelemetry APIs for manual instrumentation, allowing developers to create custom spans and events as needed.
  - See `OtelDemoApplication.kt` for an example of a tracer and an event builder initialization.
  - In the app, a custom span is emitted in `MainOtelButton.kt` after clicking on the OpenTelemetry logo button.
  - Custom events are emitted:
    - in `MainOtelButton.kt` after clicking on the OpenTelemetry logo button,
    - in `Navigation.kt` for screen changes in the app,
    - in `AstronomyShopActivity.kt` after placing an order in the shop,
    - in `Cart.kt` after emptying a cart in the shop.
  - Note: Events aren't visible in the Jaeger UI, only in the collector output.

### Known Gaps
As of now, there are a few areas where the instrumentation might not be comprehensive:

* HTTP Client Instrumentation  
OpenTelemetry Android supports automatic instrumentation for HTTP client libraries. This feature captures spans for HTTP requests with details. However, the demo app does not currently demonstrate this feature as it doesn't make any network requests.

* Disk Buffering  
Disk buffering is enabled in the app, allowing telemetry data to be temporarily stored on disk when the network is unavailable. Although this feature is configured, it isn't actively demonstrated due to the absence of network activity.

## How to use

First, start up the collector and jaeger with docker-compose:

```bash
$ docker compose build
$ docker compose up
```

Then run the demo app in the Android emulator and navigate to http://localhost:16686
to see the Jaeger UI.
