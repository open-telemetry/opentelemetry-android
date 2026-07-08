# Federated Android Semantic Conventions

This module contains Android-specific semantic convention definitions that are not currently
defined by the upstream `open-telemetry/semantic-conventions` repository, along with generated
Kotlin constants for Android-specific attributes.

These conventions are modeled as a federated semantic convention registry. The registry manifest
declares the upstream OpenTelemetry semantic conventions as a dependency so Android-specific
events can reference upstream attributes without redefining them.

The model intentionally excludes names that are already covered by upstream semantic conventions,
such as `app.crash`, `app.jank`, `app.screen.click`, and `app.widget.click`.

## Generating Attribute Constants

The Kotlin constants are generated automatically as part of the build. They're produced under:

```text
semconv/build/generated/semconv/kotlin/io/opentelemetry/android/semconv/
```

Generation is driven by the [`weaver`](https://github.com/open-telemetry/weaver) CLI, which reads the
local registry under `semconv/model` and the Kotlin templates under `semconv/templates`. You don't
need to install `weaver` yourself: a `downloadWeaver` task fetches the catalog version (see
`weaver` in `gradle/libs.versions.toml`) for your OS/architecture, verifies its checksum, and caches
it under `semconv/build/weaver/`, re-downloading only when the version changes.

Running any task that compiles the module (e.g. `./gradlew :semconv:assemble`) triggers generation
automatically. To run it explicitly:

```bash
./gradlew :semconv:generateSemanticConventions
```

Event class generation and production code usage will be added in later phases.
