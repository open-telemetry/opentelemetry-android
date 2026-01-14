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

## Android ecosystem compatibility

The android-agent currently supports the following minimum versions:

- Kotlin 1.8
- API 21+ ([desugaring of the core library](https://developer.android.com/studio/write/java8-support#library-desugaring) required for API <26)
- Android Gradle Plugin (AGP) 7.4 and Gradle 7.5
- JDK 11 (build-time)
- Java language level 8 as per [opentelemetry-java](https://github.com/open-telemetry/opentelemetry-java/blob/main/VERSIONING.md#language-version-compatibility)

These versions can be bumped in a major version release when:

1. [Google Play Services](https://developers.google.com/android/guides/setup) drops support for any of the above versions
2. A new version of Kotlin is released that does not support targeting the minimum Kotlin version
3. At the discretion of maintainers after discussing in the SIG

These are **minimum** supported versions. We would strongly recommend using newer versions where
possible as that's where our testing will be focused.

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
