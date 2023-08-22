# <img src="https://opentelemetry.io/img/logos/opentelemetry-logo-nav.png" alt="OpenTelemetry Icon" width="45" height=""> OpenTelemetry Android

:warning: This is a brand new repository that is in the process of being spun up.

## Status: Experimental

* [About](#about)
* [Getting Started](#getting-started)
* [Features](#contributing)
* [Contributing](#contributing)

# About

The repository contains the OpenTelemetry Android SDK for generating mobile
client telemetry for real user monitoring (RUM). It is built on top
of the [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java).

# Getting Started

This project is still in its infancy.

For an overview of how to contribute, see the contributing guide in [CONTRIBUTING.md](CONTRIBUTING.md).

We are also available in the [#otel-android](https://cloud-native.slack.com/archives/C05J0T9K27Q)
channel in the [CNCF slack](https://slack.cncf.io/). Please join us there for further discussions.

# Features

This android library builds on top of the [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java).
Some of the additional features provided include:

* Crash reporting
* ANR detection
* Network change detection
* Full Android Activity and Fragment lifecycle monitoring
* Access to the OpenTelemetry APIs for manual instrumentation
* SplunkRum APIs for creating custom RUM events and reporting exceptions
* Helpers to redact any span from export, or change span attributes before export
* Slow / frozen render detection
* Offline buffering of telemetry via storage

Note: Use of these features is not yet well documented.

# Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Approvers ([@open-telemetry/android-approvers](https://github.com/orgs/open-telemetry/teams/android-approvers)):

- [Jack Berg](https://github.com/jack-berg), New Relic
- [Trask Stalnaker](https://github.com/trask), Microsoft
-
## Maintainers ([@open-telemetry/java-instrumentation-maintainers](https://github.com/orgs/open-telemetry/teams/java-instrumentation-maintainers)):

- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Cesar Munoz](https://github.com/likethesalad), Elastic
