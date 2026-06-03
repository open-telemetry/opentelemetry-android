# Copilot Instructions for OpenTelemetry Android

This repository provides OpenTelemetry instrumentation for Android applications (the Android RUM SDK).

## Code Review Priorities

### Kotlin-First

This is a **Kotlin-first** project. New production code must be written in Kotlin. Java is considered unsupported for the public API — do not make special efforts to preserve Java ergonomics when the Kotlin API is clear. Flag any new `.java` source files in `src/main/` as a concern.

### Style Guide Compliance

**PRIORITY**: Verify that all code changes follow the conventions documented in [CONTRIBUTING.md](../CONTRIBUTING.md). Check:

* Code formatting — run `./gradlew spotlessApply` before every commit; CI enforces this via `./gradlew check`.
* Test framework usage:
  * **JUnit 5** for unit tests (default).
  * **JUnit 4** for Android instrumented tests and Robolectric tests (JUnit 5 is not supported there).
  * **AssertJ** for fluent assertions — use `assertThat(x).isEqualTo(y)` instead of JUnit `assertEquals`.
  * **MockK** as the mocking framework, not Mockito.
* Gradle conventions: Kotlin DSL (`build.gradle.kts`), convention plugins (`otel.android-library-conventions`, `otel.publish-conventions`).
* New instrumentation modules belong under `instrumentation/` and must follow the layout described in [WRITING_INSTRUMENTATION.md](../docs/WRITING_INSTRUMENTATION.md).

### Critical Areas

* **Public API**: Changes that alter public types, methods, or fields must update the binary compatibility golden files via `./gradlew apiDump`. Alert reviewers when `./gradlew apiCheck` would fail.
* **Performance**: Android instrumentation runs on every app frame/request. Flag hot-path allocations, unnecessary synchronization, or operations that belong off the main thread.
* **Thread Safety**: Android code runs on the main thread by default; ensure background work uses appropriate dispatchers and shared state is correctly synchronized.
* **Memory Management**: Android is memory-constrained. Flag leaks (Context references, static fields holding Activity/View), unbounded buffers, and missing resource cleanup.
* **Minimum API Level**: The SDK targets API Level 23+. Flag any usage of APIs introduced after API 23 that lack a compatibility guard.

### Quality Standards

* OpenTelemetry specification and semantic convention compliance (use the Kotlin `semconv` artifact, not the Java one).
* Proper error handling — log errors at `WARNING` level or below; do not swallow exceptions silently.
* Resource cleanup and lifecycle management (e.g. unregistering listeners in `onDetach` / `close`).
* Comprehensive unit tests for new functionality; instrumented/Robolectric tests for anything requiring Android framework behavior.
* ByteBuddy instrumentation modules must include a `testing/` app module with Android instrumented tests to validate bytecode weaving (Robolectric does not support weaving).

### PR Merge Tiers

Flag the applicable tier in your review summary:

| Tier | Scope | Approvals | Waiting period |
|------|-------|-----------|----------------|
| 1 | Dependency bumps, trivial maintenance | 1 | None |
| 2 | Bug fixes, small internal changes, new instrumentation following established patterns | 1 | None (author judgment) |
| 3 | New features, large refactors, build infra changes, wide-scope changes | 2 | ≥ 1 day |
| 4 | Public API changes (detected by `apiCheck`) | 2 | ≥ 2 days |

When a PR spans multiple tiers, apply the highest.
