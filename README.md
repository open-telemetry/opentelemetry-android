# <img src="https://opentelemetry.io/img/logos/opentelemetry-logo-nav.png" alt="OpenTelemetry Icon" width="45" height=""> OpenTelemetry Android

[![Continuous Build][ci-image]][ci-url]
[![Maven Central][maven-image]][maven-url]
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/open-telemetry/opentelemetry-android/badge)](https://scorecard.dev/viewer/?uri=github.com/open-telemetry/opentelemetry-android)
[![android api](https://img.shields.io/badge/Android_API-21-green.svg "Android min API 21")](VERSIONING.md)

## Status: development

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
> See [#73](https://github.com/open-telemetry/opentelemetry-android/issues/73) for more information.
> If your project's minSdk is lower than 24, in order to run instrumentation tests or run the app
> built on debug, you must use AGP 8.3.0+ and set the `android.useFullClasspathForDexingTransform`
> property in `gradle.properties` to `true` to ensure desugaring runs properly. For the full
> context for this workaround, please see
> [this issue](https://issuetracker.google.com/issues/230454566#comment18).

For an overview of how to contribute, see the contributing guide
in [CONTRIBUTING.md](CONTRIBUTING.md).

We are also available in the [#otel-android](https://cloud-native.slack.com/archives/C05J0T9K27Q)
channel in the [CNCF slack](https://slack.cncf.io/). Please join us there for further discussions.

## Gradle

To use this android instrumentation library in your application, first add a dependency
in your gradle build script:

```kotlin
dependencies {
    //...
    implementation("io.opentelemetry.android:android-agent:0.12.0-alpha")
    //...
}
```

# Features

This android library builds on top of
the [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java).
Some of the additional features provided include:

* [Crash reporting](./instrumentation/crash/)
* [ANR detection](./instrumentation/anr/)
* [Network change detection](./instrumentation/network/)
* Android [Activity lifecycle instrumentation](./instrumentation/activity/)
* Android [Fragment lifecycle monitoring](./instrumentation/fragment)
* [View click instrumentation](./instrumentation/view-click/)
* Access to the OpenTelemetry APIs for manual instrumentation
* Helpers to redact any span from export, or change span attributes before export
* [Slow / frozen render detection](./instrumentation/slowrendering)
* Offline buffering of telemetry via storage

Note: Use of these features is not yet well documented.

# Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

### Maintainers

- [Cesar Munoz](https://github.com/likethesalad), Elastic
- [Jason Plumb](https://github.com/breedx-splk), Splunk

For more information about the maintainer role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#maintainer).

### Approvers

- [Hanson Ho](https://github.com/bidetofevil), Embrace
- [Manoel Aranda Neto](https://github.com/marandaneto), PostHog

For more information about the approver role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#approver).

[ci-image]: https://github.com/open-telemetry/opentelemetry-android/actions/workflows/build.yaml/badge.svg

[ci-url]: https://github.com/open-telemetry/opentelemetry-android/actions?query=workflow%3Abuild+branch%3Amain

[maven-image]: https://maven-badges.sml.io/maven-central/io.opentelemetry.android/android-agent/badge.svg

[maven-url]: https://maven-badges.sml.io/maven-central/io.opentelemetry.android/android-agent
