# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Version 0.6.0 [Unreleased]
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
