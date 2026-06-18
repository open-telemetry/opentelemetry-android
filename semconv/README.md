# Federated Android Semantic Conventions

This module contains Android-specific semantic convention definitions that are not currently
defined by the upstream `open-telemetry/semantic-conventions` repository, along with generated
Kotlin constants for Android-specific attributes.

These conventions are modeled as a federated semantic convention registry. The registry manifest
declares the upstream OpenTelemetry semantic conventions as a dependency so Android-specific
events can reference upstream attributes without redefining them.

The model intentionally excludes names that are already covered by upstream semantic conventions,
such as `app.crash`, `app.jank`, `app.screen.click`, and `app.widget.click`.

## Regenerating Attribute Constants

The generated Kotlin constants are committed under:

```text
semconv/src/main/kotlin/io/opentelemetry/android/semconv/
```

Install `weaver` and make sure it is available on `PATH`, then run:

```bash
./gradlew :semconv:generateSemanticConventions
```

This task reads the local registry under `semconv/model` and the Kotlin templates under
`semconv/templates`, then regenerates the committed constants. Event class generation and
production code usage will be added in later phases.
