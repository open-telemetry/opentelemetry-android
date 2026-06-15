# Federated Android Semantic Conventions

This directory contains Android-specific semantic convention definitions that are not currently
defined by the upstream `open-telemetry/semantic-conventions` repository.

These conventions are modeled as a federated semantic convention registry. The registry manifest
declares the upstream OpenTelemetry semantic conventions as a dependency so Android-specific
events can reference upstream attributes without redefining them.

The model intentionally excludes names that are already covered by upstream semantic conventions,
such as `app.crash`, `app.jank`, `app.screen.click`, and `app.widget.click`.

Code generation and production code usage will be added in later phases.
