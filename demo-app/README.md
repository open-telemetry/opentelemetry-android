
# OpenTelemetry Android Demo App

This is an app built to demonstrate how to configure and use the OpenTelemetry Android agent
to observe app and user behavior.

This is very much a work in progress. See the `OtelDemoApplication.kt` for 
a quick and dirty example of how to get the agent initialized.

## Features

* TBD


## How to use

First, start up the collector and jaeger with docker-compose:

```bash
$ docker compose build
$ docker compose up
```

Then run the demo app in the Android emulator and navigate to http://localhost:16686
to see the Jaeger UI.
