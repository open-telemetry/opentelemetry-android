# <img src="https://opentelemetry.io/img/logos/opentelemetry-logo-nav.png" alt="OpenTelemetry Icon" width="45" height=""> OpenTelemetry Android

[![Continuous Build][ci-image]][ci-url]
[![Maven Central][maven-image]][maven-url]

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

> If your project's minSdk is lower than 26, then you must enable
> [corelib desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
> See #73 for more information.

For an overview of how to contribute, see the contributing guide in [CONTRIBUTING.md](CONTRIBUTING.md).

We are also available in the [#otel-android](https://cloud-native.slack.com/archives/C05J0T9K27Q)
channel in the [CNCF slack](https://slack.cncf.io/). Please join us there for further discussions.

## Gradle

To use this android instrumentation library in your application, first add a dependency
in your gradle build script:

```kotlin
dependencies {
    //...
    implementation("io.opentelemetry.android:instrumentation:0.1.0-alpha")
    //...
}
```


# Features

This android library builds on top of the [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java).
Some of the additional features provided include:

* Crash reporting
* ANR detection
* Network change detection
* Full Android Activity and Fragment lifecycle monitoring
* Access to the OpenTelemetry APIs for manual instrumentation
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
## Maintainers ([@open-telemetry/android-maintainers](https://github.com/orgs/open-telemetry/teams/android-maintainers)):

- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Cesar Munoz](https://github.com/likethesalad), Elastic

[ci-image]: https://github.com/open-telemetry/opentelemetry-android/actions/workflows/build.yaml/badge.svg
[ci-url]: https://github.com/open-telemetry/opentelemetry-android/actions?query=workflow%3Abuild+branch%3Amain
[maven-image]: https://maven-badges.herokuapp.com/maven-central/io.opentelemetry.android/instrumentation/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/io.opentelemetry.android/instrumentation
