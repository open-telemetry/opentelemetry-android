# OpenTelemetry Android Changelog

## Unreleased

- tbd

### Version 0.2.0 (2023-10-20)

This is a regular monthly cadence release, which follows the releases of
opentelemetry-java-instrumentation and opentelemetry-java (core/sdk).

- Update to [opentelemetry-java-instrumentation 1.31.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.31.0)
- Update to [opentelemetry-java sdk 1.31.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.31.0)
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

### 📈 Enhancements

* Update to upstream otel sdk 1.29.0 (#75)
* Add `OpenTelemetryRumBuilder.addPropagatorCustomizer()` to allow user to customize trace propagation (#71)
