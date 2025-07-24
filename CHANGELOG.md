# OpenTelemetry Android Changelog

## Unreleased

## Version 0.13.0 (2025-07-24)

- Alter FilteringSpanExporter to leverage common code from contrib
  ([#1043](https://github.com/open-telemetry/opentelemetry-android/pull/1043))
- Instrumentation docs now include installation instructions
  ([#1068](https://github.com/open-telemetry/opentelemetry-android/pull/1068))
- OpenTelemetry Android BOM now includes upstream components (instrumentation, sdk, api)
  ([#1075](https://github.com/open-telemetry/opentelemetry-android/pull/1075))
- Update docs to reflect that desugaring is required for minSdk < 26
  ([#1085](https://github.com/open-telemetry/opentelemetry-android/pull/1085))
- Include service.version in the default AndroidResource
  ([#1087](https://github.com/open-telemetry/opentelemetry-android/pull/1087))

## Version 0.12.0 (2025-07-08)

### 🌟 New instrumentation

- Capture click events for compose
  ([#1002](https://github.com/open-telemetry/opentelemetry-android/pull/1002))
- Capture click events for non-compose views
  ([#953](https://github.com/open-telemetry/opentelemetry-android/pull/953))

### 📈 Enhancements

- Agent initialization api
  ([#945](https://github.com/open-telemetry/opentelemetry-android/pull/945))
- Enable disk buffering by default in the demo app
  ([#988](https://github.com/open-telemetry/opentelemetry-android/pull/988))
- Exposing SessionProvider setter
  ([#979](https://github.com/open-telemetry/opentelemetry-android/pull/979))
- Exposing instrumentation api as agent api
  ([#1007](https://github.com/open-telemetry/opentelemetry-android/pull/1007))
- Use semantic conventions in click instrumentation
  ([#1008](https://github.com/open-telemetry/opentelemetry-android/pull/1008))
- add convenience event emitting api to OpenTelemetryRum
  ([#892](https://github.com/open-telemetry/opentelemetry-android/pull/892))

### 🧰 Tooling

- Move SessionConfig up
  ([#959](https://github.com/open-telemetry/opentelemetry-android/pull/959))
- Remove runtime dep on androidx fragment navigiation from modules that don't strictly need it
  ([#961](https://github.com/open-telemetry/opentelemetry-android/pull/961))
- Agent default instrumentation config
  ([#976](https://github.com/open-telemetry/opentelemetry-android/pull/976))
- update sonatype urls
  ([#999](https://github.com/open-telemetry/opentelemetry-android/pull/999))

## Version 0.11.0 (2025-04-15)

### 📣 Migration notes

Please be aware that the maven coordinates for many instrumentation modules
have changed. Details can be found [here](https://github.com/open-telemetry/opentelemetry-android/pull/926).

### ⚠️⚠️ Breaking changes

- Remove `setSessionTimeout()` on `OtelRumConfig` in favor of new `setSessionConfig()`.([#887](https://github.com/open-telemetry/opentelemetry-android/pull/887))
- Update Fragment and Activity attribute names. ([#920](https://github.com/open-telemetry/opentelemetry-android/pull/920))

### 🌟 New instrumentation

- Generate events for OkHttp Websocket events
  ([#863](https://github.com/open-telemetry/opentelemetry-android/pull/863))**
- Add build-time `android.util.Log` call-site substitutions
  ([#911](https://github.com/open-telemetry/opentelemetry-android/pull/911))

### 📈 Enhancements

- Support custom attribute extractors to auto-http instrumentations
  ([#867](https://github.com/open-telemetry/opentelemetry-android/pull/867))
- Allow users to configure suppression of some instrumentations.
  ([#883](https://github.com/open-telemetry/opentelemetry-android/pull/883))
- Use event name for crash event (instead of attr)
  ([#894](https://github.com/open-telemetry/opentelemetry-android/pull/894))
- Migrate network change event from zero-duration span to (log-based) event.
  ([#895](https://github.com/open-telemetry/opentelemetry-android/pull/895))

### 🛠️ Bug fixes

- Fix instrumentation publication collisions
  ([#926](https://github.com/open-telemetry/opentelemetry-android/pull/926))

## Version 0.10.0 (2025-03-06)

- This version builds on opentelemetry-java-instrumentation
  [v2.13.3](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.13.3).

### ⚠️⚠️ Breaking changes

- New maven coordinates for http client instrumentations ([#791](https://github.com/open-telemetry/opentelemetry-android/pull/791))
  - `okhttp-3.0-library` -> `instrumentation-okhttp-3.0-library`
  - `okhttp-3.0-agent` -> `instrumentation-okhttp-3.0-agent`
  - `httpurlconnection-library` -> `instrumentation-httpurlconnection-library`
  - `httpurlconnection-agent` -> `instrumentation-httpurlconnection-agent`
- Remove deprecated `exception.escaped` attribute from crash events ([#796](https://github.com/open-telemetry/opentelemetry-android/pull/796))
- `DiskBufferingConfiguration` renamed to `DiskBufferingConfig` ([#753](https://github.com/open-telemetry/opentelemetry-android/pull/753))
- Remove `ServiceManager` instance from `InstallationContext` ([#763](https://github.com/open-telemetry/opentelemetry-android/pull/763))
- Remove hard-coded `exception.escaped` attribute from crashes ([#796](https://github.com/open-telemetry/opentelemetry-android/pull/796))
- Drop support for Kotlin 1.7 ([#869](https://github.com/open-telemetry/opentelemetry-android/pull/869))

### 📈 Enhancements

- The android-agent module now publishes a Bill of Materials (BOM).
  This BOM can be used to coordinate platform dependency versions across the various
  modules contained in opentelemetry-android ([#809](https://github.com/open-telemetry/opentelemetry-android/pull/809))
- Add ability to enable verbose debug for disk buffering config ([#753](https://github.com/open-telemetry/opentelemetry-android/pull/753))
- Ensure current screen attribute is included in logs, when configured ([#785](https://github.com/open-telemetry/opentelemetry-android/pull/785))
- Default max cache size for disk buffering reduced from 60MB to 10MB ([#822](https://github.com/open-telemetry/opentelemetry-android/pull/822))
- Improve concurrency/threading for initialization events ([#836](https://github.com/open-telemetry/opentelemetry-android/pull/836))
- Remove minimum disk buffering cache size requirement and pre-allocation ([#828](https://github.com/open-telemetry/opentelemetry-android/pull/828))
- Add ability to customize the directory used for disk buffering ([#871](https://github.com/open-telemetry/opentelemetry-android/pull/871))

## Version 0.9.0 (2025-01-15)

- This version builds on opentelemetry-java-instrumentation
  [v2.11.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.11.0).

### ⚠️⚠️Breaking changes

A reminder that this project is still alpha and may contain breaking changes
from release-to-release prior to v1.0.0.

- The `AndroidInstrumentation.install()` method signature has changed. Any 3rd-party
  instrumentation written to this interface will need to be updated.
  ([#671](https://github.com/open-telemetry/opentelemetry-android/pull/671))

### 📈 Enhancements

- Add the current screen name to crash events.
  ([#704](https://github.com/open-telemetry/opentelemetry-android/pull/704))
- Add R8 consumer rules.
  ([#685](https://github.com/open-telemetry/opentelemetry-android/pull/685))
- Append the session id attribute to all LogRecords.
  ([#697](https://github.com/open-telemetry/opentelemetry-android/pull/697))
- Add support for wired network types in the network detector.
  ([#673](https://github.com/open-telemetry/opentelemetry-android/pull/673))
- Add ability to generate session start/end events. This feature is currently opt-in.
  ([#717](https://github.com/open-telemetry/opentelemetry-android/pull/717),
   [#719](https://github.com/open-telemetry/opentelemetry-android/pull/719))
- Support newer Android network APIs for API >=29.
  ([#736](https://github.com/open-telemetry/opentelemetry-android/pull/736))

## Version 0.8.0 (2024-10-18)

### 📈 Enhancements

- HttpURLConnection instrumentation migration to AutoService API
  ([#592](https://github.com/open-telemetry/opentelemetry-android/pull/592))
- Make HttpURLConnection connection inactivity timeout configurable and add test for harvester code
  ([#569](https://github.com/open-telemetry/opentelemetry-android/pull/569))
- Expose additional disk buffering configuration
  ([#596](https://github.com/open-telemetry/opentelemetry-android/pull/596))
- Many enhancements to the Android
  [demo-app](https://github.com/open-telemetry/opentelemetry-android/tree/main/demo-app).
  [#545](https://github.com/open-telemetry/opentelemetry-android/pull/545),
  [#554](https://github.com/open-telemetry/opentelemetry-android/pull/554),
  [#568](https://github.com/open-telemetry/opentelemetry-android/pull/568),
  [#570](https://github.com/open-telemetry/opentelemetry-android/pull/570),
  [#577](https://github.com/open-telemetry/opentelemetry-android/pull/577),
  [#584](https://github.com/open-telemetry/opentelemetry-android/pull/584),
  [#598](https://github.com/open-telemetry/opentelemetry-android/pull/598),
  [#604](https://github.com/open-telemetry/opentelemetry-android/pull/604),
  [#605](https://github.com/open-telemetry/opentelemetry-android/pull/605),
  [#627](https://github.com/open-telemetry/opentelemetry-android/pull/627),
  [#634](https://github.com/open-telemetry/opentelemetry-android/pull/634)

### 🛠️ Bug fixes
- Ending "Paused" span for a fragment.
  ([#591](https://github.com/open-telemetry/opentelemetry-android/pull/591))
- start AppStart span when installing activity instrumentation
  ([#578](https://github.com/open-telemetry/opentelemetry-android/pull/578))


## Version 0.7.0 (2024-08-14)

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
