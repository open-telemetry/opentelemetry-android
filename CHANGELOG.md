# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Version 0.17.0

* TBD

## Version 0.16.0

* Updated to OpenTelemetry Java v1.15.0 (#303)
* Fix race condition in slow render detection (#304)

## Version 0.15.0

* Updated to OpenTelemetry Java v1.14.0 (#287)

## Version 0.14.0

* Disk caching exporter now retries sending files (#260)
* Add ability to customize `screen.name` attribute with `@RumScreenName` annotation (#261)
* Add ability to limit storage usage for buffered telemetry (#272)
* Add method to check if RUM library has been initialized (#273)
* Add option to sample traces based on session ID (#273)

## Version 0.13.0

* Update RUM property to support GDI spec 1.2 (#198)
* Add `exception.type` and `exception.message` for crash report spans (#200)
* Initial support for Volley HurlStack instrumentation (#209)
* Support for detecting slow and frozen renders, enabled by default (#236)
* Sample app updated to support slow renders (#236)
* Updated to OpenTelemetry Java v1.12.0 (#254)
* Add experimental support of buffering telemetry through storage (#251)
* Consistency improvements to public configuration API (#255)
* Add session timeout after a period of inactivity (#226)
* Numerous dependency upgrades

---
## Version 0.12.0
- BUGFIX: Initialization events now share the same clock instance.
- The `beaconEndpoint` configuration setting always overrides the `realm` setting.

---
## Version 0.11.0
- BUGFIX: Fixed another issue that could service if the `ConnectivityManager` threw an exception when queried.
  See the corresponding Android bug: https://issuetracker.google.com/issues/175055271
- ANR spans are now properly marked as ERROR spans.
- Library now targets SDK version 31 (minimum version is still 21)
- The opentelemetry-okhttp-3.0 instrumentation has been updated to version 1.6.2.

---
## Version 0.10.0

- BUGFIX: Fixed a bug that could crash the application if Android's `ConnectivityManager` threw an
exception when queried. See the corresponding Android bug: https://issuetracker.google.com/issues/175055271
- Updated OpenTelemetry okhttp instrumentation to v1.6.0.
- Capture attributes related to OkHttp Exceptions in the http client instrumentation.

---

## Version 0.9.0

- A span is now created when the SessionId changes to link the new session to the old one. The exact
  details of this span will probably change in the future.
- The library has been updated to use OpenTelemetry Java v1.6.0.
- All span string-valued attributes will now be truncated to 2048 characters if they exceed that
  limit.

---

## Version 0.8.0

- Fixed a `NullPointerException` that sometimes happened during the Network monitor initialization.
- The Zipkin exporter is now lazily initialized in a background thread. This change should greatly speed up the library startup.

---

## Version 0.7.0

- OpenTelemetry okhttp instrumentation has been updated to version 1.5.3-alpha.
- For okhttp, SplunkRum now exposes a wrapper for your `OkHttpClient` which implements the `Call.Factory`
  interface. This `Call.Factory` will properly manage context propagation with asynchronous http calls.
- The okhttp Interceptor provided by SplunkRum has been deprecated. Please use the `Call.Factory` from now on.
  The `createOkHttpRumInterceptor()` method will be removed in a future release.
- A new class (`com.splunk.rum.StandardAttributes`) has been introduced to provide `AttributeKey`s for
  standard RUM span attributes. Currently this class exposes the `APP_VERSION` attribute.
- The ANR detector and Network monitor will no longer operate when the app has been put in the background.
- A new API on the `Config.Builder` allows redacting of spans or redacting/replacing Span attributes. See
  the new `filterSpans(Consumer<SpanFilterBuilder>)` method and the corresponding `com.splunk.rum.SpanFilterBuilder` class
  for details.
- The `os.type` Span attribute has been changed to 'linux' and `os.name` attribute is now 'Android'.

---

## Version 0.6.0

- Adds proguard consumer information to assist with proguarded release builds.

---

## Version 0.5.0

- The initial cold `AppStart` span now starts with the library initialization and continues until the first Activity has been restored.
- Span names now have their capitalization preserved, rather than being lower-cased everywhere.

---

## Version 0.4.0

- All methods deprecated in v0.3.0 have been removed.
- The span names generated for Activity/Fragment lifecycle events no longer include the
  Activity/Fragment name as a prefix. There is still an attribute which tracks the name.

---

## Version 0.3.0

- The `com.splunk.rum.Config.Builder` class has been updated.
  - The `beaconUrl(String)` method has been deprecated and replaced with `beaconEndpoint(String)`.
  - A new `realm(String)` method has been added for easier beacon endpoint configuration.
  - The `rumAuthToken(String)` method has been deprecated and replaced with `rumAccessToken(String)`
    .
  - A new `deploymentEnvironment(String)` method has been added as a helper to set your deployment
    environment value.
- The method for recording exceptions has changed slightly:
  - The method that took a `String name` parameter has been deprecated.
  - New methods have been added that use the exception class name as the name of the Span.
- The `last.screen.name` attribute will only be recorded during detected screen transitions.
- New methods have been added to the `SplunkRum` API to allow updating the "global" attributes that
  are added to every span and event.

---
## Version 0.2.0

- Instrumentation has been updated to use OpenTelemetry v1.4.1
- ANRs are now detected by the Instrumentation and will be reported as "ANR" spans.
- A new API has been added to track timed RUM "workflows" as OpenTelemetry Span instances.
- The values reported for network types have been updated to match OpenTelemetry semantic
  conventions.
- The SplunkRum class has had a method added to return a no-op implementation of the SplunkRum
  capabilities.
- The SplunkRum initialization span now includes an attribute describing the features that have been
  configured.
- The instrumentation now tracks 3 types of AppStart spans: cold, hot and warm. Note that "hot"
  starts are not tracked for multi-Activity apps, only single-Activity.

---
## Version 0.1.0

This is the first official beta release of the project.
