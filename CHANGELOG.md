# OpenTelemetry Android Changelog

## Unreleased

### 🚧 Refactorings

- Implementing an instrumentation API to handle auto instrumentations.
  ([#396](https://github.com/open-telemetry/opentelemetry-android/pull/396)) This change included:
    - The old module `android-agent` was renamed to `core` and a new `android-agent` module was
      created to bring together the core functionalities plus the default instrumentations.
    - The following modules were refactored to implement the new `AndroidInstrumentation` api and to
      invert their dependency with the `core` module so that the `core` isn't aware of
      them: `activity`, `anr`, `crash`, `fragment`, `network`, `slowrendering`, `startup`.
    - (Breaking) The config options related to auto instrumentations that used to live
      in `OtelRumConfig` were move to each instrumentation's `AndroidInstrumentation`
      implementation. This means that the way to configure auto instrumentations now must be done
      via
      the `AndroidInstrumentationLoader.getInstrumentation(AndroidInstrumentationImpl::class.java)`
      method where `AndroidInstrumentationImpl` must be replaced by the implementation type that
      will be configured. Each implementation should contain helper functions (setters, adders, etc)
      to allow configuring itself whenever needed.

### 🌟 New instrumentation

- Http/sURLConnection auto instrumentation.
  ([#133](https://github.com/open-telemetry/opentelemetry-android/pull/133))

### 📈 Enhancements

- Logs are now exported to stdout by
  default. ([#424](https://github.com/open-telemetry/opentelemetry-android/pull/424))
- New method to customize log exporter:
  addLogRecordExporterCustomizer() ([#424](https://github.com/open-telemetry/opentelemetry-android/pull/424))
- Adding RUM initialization
  events. ([#397](https://github.com/open-telemetry/opentelemetry-android/pull/397))
- Upgrading Kotlin to 2.0.0
  ([#388](https://github.com/open-telemetry/opentelemetry-android/pull/388))
- Adding Hanson and Manoel as approvers.
  ([#413](https://github.com/open-telemetry/opentelemetry-android/pull/413))

### 🧰 Tooling

- Not adding artifacts to the GH release page.
  ([#385](https://github.com/open-telemetry/opentelemetry-android/pull/385))
- Populating the session id on screen for the demo app.
  ([#402](https://github.com/open-telemetry/opentelemetry-android/pull/402))
- Setting up docker compose files for the demo app.
  ([#426](https://github.com/open-telemetry/opentelemetry-android/pull/426))
- Running android tests as part of daily checks.
  ([#509](https://github.com/open-telemetry/opentelemetry-android/pull/509))
- Adding a cart to the demo app.
  ([#518](https://github.com/open-telemetry/opentelemetry-android/pull/518))
- Demo app improvements.
  ([#497](https://github.com/open-telemetry/opentelemetry-android/pull/497),
  [#507](https://github.com/open-telemetry/opentelemetry-android/pull/507),
  [#414](https://github.com/open-telemetry/opentelemetry-android/pull/414))

## Version 0.6.0 (2024-05-22)

This version of OpenTelemetry Android is built on:

* OpenTelemetry Java
  Instrumentation [2.4.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.4.0)
* OpenTelemetry Java
  Contrib [1.34.0-alpha](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/tag/v1.35.0)
* OpenTelemetry
  SDK [1.38.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.38.0)

### 🌟 New instrumentation

- Experimental Volley http client
  instrumentation [#291](https://github.com/open-telemetry/opentelemetry-android/pull/291).

### 📈 Enhancements

- There is now an initial version of an OpenTelemetry Android demo
  app. [#338](https://github.com/open-telemetry/opentelemetry-android/pull/338)
- Session timeout duration is now configurable beyond the 15 minute
  default [#330](https://github.com/open-telemetry/opentelemetry-android/pull/330)

### 🛠️ Bug fixes

- Scheduled components now use fixed delay instead of fixed
  rate [#332](https://github.com/open-telemetry/opentelemetry-android/pull/332).

### 🧰 Tooling

- A variety of small tweaks to the build process to make it smoother and more consistent with other
  OpenTelemetry Java repos.

## Version 0.5.0 (2024-04-23)

⚠️⚠️⚠️ There are considerable breaking changes in this release.

Breaking changes include considerable restructuring of the overall project layout. This provides a
much more modularized project that publishes more granular instrumentation modules. Note that as a
result of this, the topmost dependency is changing its name
to `io.opentelemetry.android:android-agent`.

### 📈 Enhancements

- Append global attributes to logs signal.
  ([#266](https://github.com/open-telemetry/opentelemetry-android/pull/266))
- Change crash reporting to send a LogRecord instead of Span.
  ([#237](https://github.com/open-telemetry/opentelemetry-android/pull/237))
- Restructure
  modules ([#267](https://github.com/open-telemetry/opentelemetry-android/pull/267), [#269](https://github.com/open-telemetry/opentelemetry-android/pull/269),
  and [#276](https://github.com/open-telemetry/opentelemetry-android/pull/276))
- Update upstream deps
  ([#301](https://github.com/open-telemetry/opentelemetry-android/pull/301)
  and [#304](https://github.com/open-telemetry/opentelemetry-android/pull/304))
- Update README re: desugaring
  ([#309](https://github.com/open-telemetry/opentelemetry-android/pull/309))

### 🛠️ Bug fixes

- Ensure that services are initialized via ServiceManager when `OpenTelemetryRum` is built.
  ([#272](https://github.com/open-telemetry/opentelemetry-android/pull/272))
- Start the `ServiceManager` itself when `OpenTelemetryRum` is built.
  ([#278](https://github.com/open-telemetry/opentelemetry-android/pull/278))

### 🧰 Tooling

- Update Release process
  ([#300](https://github.com/open-telemetry/opentelemetry-android/pull/300))
- Adding '-alpha' to all modules' versions
  ([#297](https://github.com/open-telemetry/opentelemetry-android/pull/297))

## Version 0.4.0 (2024-03-04)

- Update
  to [opentelemetry-java-instrumentation 1.32.1](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.32.1)
- Update
  to [opentelemetry-java sdk 1.35.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.35.0)
- Wire up support for ANRs, crash reporting, and slow rendering detection, with configurability
  support (#192)
- Fix okhttp instrumentation to include known http methods (#215)
- Finish adding initial implementation of through-disk buffering support (#194, #221)

## Version 0.3.0 (2023-12-13)

- Update
  to [opentelemetry-java-instrumentation 1.32.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.32.0)
- Update
  to [opentelemetry-java sdk 1.33.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.33.0)
- Stabilizing support for okhttp automatic build-time instrumentation (#159)

## Version 0.2.0 (2023-10-20)

This is a regular monthly cadence release, which follows the releases of
opentelemetry-java-instrumentation and opentelemetry-java (core/sdk).

- Update
  to [opentelemetry-java-instrumentation 1.31.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.31.0)
- Update
  to [opentelemetry-java sdk 1.31.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.31.0)
- BREAKING - Update to latest java semantic conventions (#114)
    - `net.host.connection.type` -> `network.connection.type`
    - `net.host.carrier.icc` -> `network.carrier.icc`
    - `net.host.carrier.mcc` -> `network.carrier.mcc`
    - `net.host.carrier.mnc` -> `network.carrier.mnc`
    - `net.host.carrier.name` -> `network.carrier.name`
    - `net.host.connection.type` -> `network.connection.type`
    - `net.host.connection.subtype` -> `network.connection.subtype`
- Add experimental support for okhttp automatic build-time instrumentation (#64, #110)

## Version 0.1.0 (2023-09-13)

This version marks the first baseline release of `opentelemetry-android` instrumentation.
This project is classified as experimental.

## 📈 Enhancements

* Update to upstream otel sdk 1.29.0 (#75)
* Add `OpenTelemetryRumBuilder.addPropagatorCustomizer()` to allow user to customize trace
  propagation (#71)
