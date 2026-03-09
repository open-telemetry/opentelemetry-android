# AI Agent Guidelines for opentelemetry-android

This file provides instructions for AI coding agents working on this repository.
AI agents that create pull requests **MUST** follow these rules.

## Before You Start: Understand the Codebase

This is an OpenTelemetry SDK for Android, built on the OpenTelemetry Java SDK. It is
published to Maven Central and has an established architecture. Do not treat it as a
greenfield codebase.

Before proposing any change, you must:

1. **Read the module architecture below** and understand where your change fits.
2. **Search the existing code** for functionality related to your change. Features you think
   are missing may already exist in a different module or under a different name.
3. **Read `CONTRIBUTING.md`** for PR process, code conventions, and testing requirements.

Many AI-generated PRs have been rejected because they re-implemented functionality that already
existed or proposed an approach that didn't account for how the project is actually structured.

## Module Architecture

The modules form a layered architecture. Lower layers must not depend on higher layers:

```
android-agent          (opinionated setup: core + default instrumentations + Kotlin DSL config)
    |
    +-- core           (OTel Java SDK setup, builders, exporters, processors)
    |     |
    |     +-- instrumentation/android-instrumentation  (base instrumentation API)
    |     +-- common        (shared internal utils, RumConstants, network data types)
    |     +-- services      (singleton wrappers around Android SDK APIs)
    |     +-- session       (session API: Session, SessionProvider, SessionObserver)
    |     +-- agent-api     (public API: OpenTelemetryRum interface)
    |
    +-- instrumentation/*   (individual instrumentations, each in its own module)
```

Key points:

- **`android-agent`** is the batteries-included entry point. It bundles `core` plus all default
  instrumentations and provides a Kotlin DSL for configuration. Most end-user-facing features
  are already wired here.
- **`core`** configures the OTel Java SDK (TracerProvider, MeterProvider, LoggerProvider).
  It uses `OpenTelemetryRumBuilder` and `SdkPreconfiguredRumBuilder` for two initialization paths.
- **`instrumentation/*`** modules each implement the `AndroidInstrumentation` interface and are
  discovered at runtime via `@AutoService`. Some instrumentations (okhttp3, compose,
  httpurlconnection, android-log) require **bytecode weaving** via a ByteBuddy Gradle plugin —
  they cannot work as simple runtime dependencies.
- **`session`** already provides `SessionProvider`, `SessionPublisher`, and `SessionObserver`.
  Session IDs are already injected into spans via processors in `core`.

## PR Requirements

### Size: Keep PRs Small

**Pull requests must not exceed ~500 changed lines.** Large PRs are extremely difficult to review
and will be sent back. If your change is bigger than that, break it into smaller, independently
reviewable PRs that each make one clear, incremental change.

### Scope: One Purpose Per PR

Each PR should have a single purpose. Do not combine unrelated changes (e.g., a bug fix and a
refactor, or a new feature and code cleanup). Keep the PR focused on one thing.

### Accuracy: Verify Your Change Is Actually Needed

Before writing code, confirm that:

- The feature or fix does not already exist in the codebase.
- Your approach is compatible with the existing architecture.
- You are modifying the correct module(s) for the change.
- If you are adding public API, it is truly necessary and not an implementation detail that
  should remain `internal`.

### Public API Surface

Any change to the public API surface is detected by `apiCheck` and requires extra scrutiny
(see `CONTRIBUTING.md` for the approval process). Before adding public types, methods, or fields:

- Prefer `internal` visibility unless there is a clear end-user need.
- Do not expose concrete implementation classes; prefer interfaces.
- Do not expose factories or configuration classes that are implementation details.
- Data classes are [discouraged in public APIs](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html#avoid-using-data-classes-in-your-api).

## Code Change Discipline

- **Only change what is necessary.** Do not refactor, reformat, rename, or restructure existing
  code that is unrelated to your task. This includes comments — do not reword, add, or remove
  comments that are not directly related to the PR's goal. If a line is not functionally
  affected by your change, leave it exactly as-is.
- Preserve the surrounding code's style and idioms.
- Do not add redundant comments that just narrate what the code does.

## Code Style and Quality

- **Language**: Kotlin preferred, Java acceptable for existing Java files.
- **Formatting**: Run `./gradlew spotlessApply` before submitting.
- **Binary compatibility**: Run `./gradlew apiCheck`. If you intentionally changed API,
  run `./gradlew apiDump` and include the updated `.api` files in the PR.

## Testing

- Use **JUnit 5** (Jupiter) for unit tests.
- Use **Robolectric** when Android framework classes are needed. Robolectric tests must use
  **JUnit 4** since Robolectric is not compatible with JUnit 5.
- Use **MockK** (not Mockito) for mocking.
- Use **AssertJ** for assertions (`assertThat(...).isEqualTo(...)`, not `assertEquals`).
- Tests must pass: `./gradlew check`.

## What Will Get Your PR Rejected

- PRs over ~500 changed lines.
- Re-implementing functionality that already exists in the codebase.
- Adding public API that should be `internal`.
- Not understanding the module layering (e.g., putting agent-level code in core).
- Proposing changes that don't work with the existing architecture.
- Bundling unrelated changes into a single PR.
- Skipping tests or formatting.
