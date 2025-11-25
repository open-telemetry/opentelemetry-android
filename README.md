# <img src="https://opentelemetry.io/img/logos/opentelemetry-logo-nav.png" alt="OpenTelemetry Icon" width="45" height=""> OpenTelemetry Android

[![Continuous Build][ci-image]][ci-url]
[![Maven Central][maven-image]][maven-url]
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/open-telemetry/opentelemetry-android/badge)](https://scorecard.dev/viewer/?uri=github.com/open-telemetry/opentelemetry-android)
[![android api](https://img.shields.io/badge/Android_API-21-green.svg "Android min API 21")](VERSIONING.md)

* [About](#about)
* [Getting Started](#getting-started)
* [Features](#features)
* [Contributing](#contributing)

# About

The repository contains the `OpenTelemetry Android Agent`, which initializes the [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java) and provides
auto-instrumentation of Android apps for real user monitoring (RUM).

While this project isn't 100% Kotlin, it has a "Kotlin-First" policy where usage in Kotlin-based Android apps will be prioritized in terms
of API and idioms. More details about this policy can be found [here](./docs/KOTLIN_FIRST.md).

# Getting Started

> If your project's minSdk is lower than 26, then you must enable
> [corelib desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
> See [#73](https://github.com/open-telemetry/opentelemetry-android/issues/73) for more information.
> Further, you must use AGP 8.3.0+ and set the `android.useFullClasspathForDexingTransform`
> property in `gradle.properties` to `true` to ensure desugaring runs properly. For the full
> context for this workaround, please see
> [this issue](https://issuetracker.google.com/issues/230454566#comment18).

## Gradle Setup

To use the Android Agent in your application, you will first need to add
a dependency in your application's `build.gradle.kts`. We publish a bill of materials (BOM) that
helps to coordinate versions of the this project's components and the upstream
`opentelemetry-java-instrumentation` and `opentelemetry-java` dependencies. We recommend
using the BOM as a platform dependency, and then omitting explicit version information
from all other opentelemetry dependencies:

```kotlin
dependencies {
    //...
    api(platform("io.opentelemetry.android:opentelemetry-android-bom:1.0.0-rc.1-alpha"))
    implementation("io.opentelemetry.android:android-agent") // Version is resolved through the BOM
    //...
}
```

## Agent Initialization

To initialize the Agent, call `OpenTelemetryRumInitializer.initialize()` in the `onCreate()` function in your app's `Application` object, ideally as early as possible after calling `super.onCreate()`.

```kotlin
class MainApplication: Application() {
    var otelRum: OpenTelemetryRum? = null
    
    override fun onCreate() {
        super.onCreate()
        otelRum = initOTel(this)
    }
}

private fun initOTel(context: Context): OpenTelemetryRum? =
    runCatching {
        OpenTelemetryRumInitializer.initialize(
            context = context,
            configuration = {
                httpExport {
                    baseUrl = "http://10.0.2.2:4318"
                    baseHeaders = mapOf("foo" to "bar")
                }
                instrumentations {
                    activity {
                        enabled(true)
                    }
                    fragment {
                        enabled(false)
                    }
                }
                session {
                    backgroundInactivityTimeout = 5.minutes
                    maxLifetime = 1.days
                }
                globalAttributes {
                    Attributes.of(stringKey("demo-version"), "test")
                }
            }
        )
    }.onFailure {
        Log.e("OpenTelemetryRumInitializer", "Initialization failed", it)
    }.getOrNull()
```

This call will return an `OpenTelemetryRum` instance with which you can use the Agent and OTel APIs.

# Features

In addition to exposing the OTel Java API for manual instrumentation, agent also offers the following features:

* Streamlined initialization and configuration of the Java SDK instance
* Installation and management of bundled instrumentation
* Offline buffering of telemetry via disk persistence
* Redact and change span attributes before export

## Instrumentation

The following instrumentation modules are bundled with the Android Agent:

* [Activity lifecycle](./instrumentation/activity/)
* [ANR detection](./instrumentation/anr/)
* [Crash reporting](./instrumentation/crash/)
* [Fragment lifecycle](./instrumentation/fragment)
* [Network change detection](./instrumentation/network/)
* [Slow/frozen frame render detection](./instrumentation/slowrendering)
* [Startup](./instrumentation/startup)
* [Sessions](./instrumentation/sessions)
* [Screen orientation](./instrumentation/screen-orientation)
* [View click](./instrumentation/view-click/)

There are also other
[additional instrumentation modules](https://github.com/open-telemetry/opentelemetry-android/tree/main/instrumentation)
that application developers can include through a gradle dependency. 
Instrumentations are detected at runtime via the classpath, and
are installed automatically.

## Additional Documentation

See the following pages for details about the related topics:

- [Kotlin-First Policy](./docs/KOTLIN_FIRST.md)
- [StrictMode Guidance](./docs/STRICTMODE.md)
- [Exporter Management](./docs/EXPORTER_CHAIN.md)

# Contributing

For an overview of how to contribute, see the contributing guide in [CONTRIBUTING.md](CONTRIBUTING.md).

We are also available in the [#otel-android](https://cloud-native.slack.com/archives/C05J0T9K27Q)
channel in the [CNCF Slack](https://slack.cncf.io/). Please join us there for further discussions.

## Maintainers

- [Cesar Munoz](https://github.com/likethesalad), Elastic
- [Jason Plumb](https://github.com/breedx-splk), Splunk

For more information about the maintainer role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#maintainer).

## Approvers

- [Hanson Ho](https://github.com/bidetofevil), Embrace
- [Jamie Lynch](https://github.com/fractalwrench), Embrace
- [Manoel Aranda Neto](https://github.com/marandaneto), PostHog

For more information about the Approver role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#approver).

[ci-image]: https://github.com/open-telemetry/opentelemetry-android/actions/workflows/build.yaml/badge.svg

[ci-url]: https://github.com/open-telemetry/opentelemetry-android/actions?query=workflow%3Abuild+branch%3Amain

[maven-image]: https://maven-badges.sml.io/maven-central/io.opentelemetry.android/android-agent/badge.svg

[maven-url]: https://maven-badges.sml.io/maven-central/io.opentelemetry.android/android-agent
