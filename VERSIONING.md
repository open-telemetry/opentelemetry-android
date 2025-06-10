# OpenTelemetry Android Versioning

This document address a variety of versioning and release considerations for
the OpenTelemetry Android project.

## Versioning scheme

This codebase uses [Semantic Versioning](https://semver.org/) (semver) for its version numbers.
All modules in this codebase are released at the same time and, as such, will
be versioned together. All modules in this repo are released with the same version number.

Until 1.0.0 stability has been achieved, regular releases will only typically increment
the minor version (second number in the semver triplet). Patch releases are considered
exceptional, and will only be created if a critical issue needs to be addressed shortly after
a regular release.

## Snapshot builds

Every commit to the `main` branch will cause a
[snapshot build](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/io/opentelemetry/android/)
to be published to Sonatype. Users may choose to build and test and file issues against SNAPSHOT
builds, but their use in production is strongly discouraged.

## Android API Compatibility

The core Android instrumentation provided here is designed to be used with
Android API Level 21 and above. API levels 21 to 25 require
[desugaring of the core library](https://developer.android.com/studio/write/java8-support#library-desugaring).
The desugaring library should be version 2.0.4 or newer, and the Android Gradle Plugin (AGP)
should be version 7.4 or newer. You can consult the
[Android docs for Gradle and AGP compatibility](https://developer.android.com/build/releases/gradle-plugin#updating-gradle)

The API compatibility level outlined here is aligned with the Android compatibility
in [opentelemetry-java](https://github.com/open-telemetry/opentelemetry-java/blob/main/VERSIONING.md#language-version-compatibility).
The minimum compatibility version is subject to change when the minimum requirement
for [Google Play Services](https://developers.google.com/android/guides/setup) changes.

The various `auto-instrumentation` modules provided here are NOT required to follow this
same compatibility level. That is, certain auto-instrumentation modules MAY choose to use a
newer minimum Android SDK level. When an instrumentation differs from the default,
the minimum supported sdk will be listed in the `build.gradle.kts` file for that module. For example
`otelAndroid.minSdk = 26` would indicate that the instrumentation requires a minimum sdk version 26
to be used.

## Release schedule

This Android library is built on top of other OpenTelemetry components:
* [opentelemetry-java](https://github.com/open-telemetry/opentelemetry-java)
* [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
* [opentelemetry-java-contrib](https://github.com/open-telemetry/opentelemetry-java-contrib)

As such, this project will follow a release schedule that is related to the upstream release
schedule. Presently, this means a monthly release, typically within a week of the last
release of the above components.

## Internal packages

Java and/or Kotlin code that lives in a package with `internal` anywhere in the name
should be considered internal to the project. Code in `internal` packages is not intended
for direct use, even if classes or methods may be public.

Internal code may change at any time and carries no API stability constraints.
