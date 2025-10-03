# instrumentation-all (aggregate)

Status: development

The `instrumentation-all` module aggregates all currently available end‑user Android instrumentation libraries in this repository
behind a single dependency coordinate. It is intended for quick evaluation, prototyping, demo apps, or exploratory
testing—NOT for long‑term production builds (where you should declare only what you use to minimize size, method count,
and initialization overhead).

## When to use

Use this module when you want to see “everything working” without hunting down each instrumentation artifact. Once you
decide which signals you actually want, replace this aggregate with the individual modules.

## Installation

Add the BOM and the aggregate artifact:

```kotlin
dependencies {
    api(platform("io.opentelemetry.android:opentelemetry-android-bom:<version>"))
    implementation("io.opentelemetry.android:instrumentation-all:<version>")
}
```

Replace `<version>` with the desired release (the aggregate will follow the repository's published versioning). If you
already use `android-agent`, you typically do NOT also need `instrumentation-all`; `instrumentation-all` is mostly for
manual / modular setups or experimentation.

## Included (at time of writing)

Feature / library instrumentation modules bundled:

* activity
* anr
* crash
* fragment
* network
* slowrendering
* sessions
* startup
* view-click
* compose:click
* okhttp3 (library)
* okhttp3-websocket (library)
* httpurlconnection (library)
* android-log (library)

Supporting foundational modules:

* common-api
* android-instrumentation

Excluded by design:

* `*:agent` variants (bytecode / agent specific, not needed in normal Android app packaging)
* `*:testing` helpers

## Telemetry

This module does not produce telemetry on its own—it simply re-exports other instrumentation modules. See the
individual instrumentation READMEs for emitted spans/logs/metrics.

## Maintenance / Updating

When a new instrumentation module is added to the repository, append an `api(project("..."))` entry inside
`instrumentation/all/build.gradle.kts` and update the lists above. PRs that add new instrumentation should include that
change.

## Production Guidance

Avoid depending on this aggregate in production apps: it can increase APK size, method count, memory, and startup work
by pulling in features you may not enable. Prefer explicit, minimal dependencies.
