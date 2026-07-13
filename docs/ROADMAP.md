# OpenTelemetry Android Roadmap

This document outlines the roadmap for OpenTelemetry Android. It describes the 
categorical topics of interest and the areas were development should focus.

This roadmap does not contain timelines or due dates. The speed of project
advancement is primarily limited by contributor availability.

To the reader: This document is likely broader than our 
[current set of GitHub issues](https://github.com/open-telemetry/opentelemetry-android/issues).
If you feel that a specific area isn't well captured, pleas feel free to
[open a new issue](https://github.com/open-telemetry/opentelemetry-android/issues/new)
to discuss and work on it.

## Table of Contents:

* [Stability](#stability)
  * [API Stability](#api-stability)
  * [Instrumentation Stability](#instrumentation-stability)
  * [Semantic Convention Stability](#semantic-convention-stability)
* [Agent Enhancements](#agent-enhancements)
* Android and Kotlin compatibility
* Declarative Configuration
* Documentation
* Instrumentation Enhancements

## Stability

Declarations of stability are one of the most important aspects of this project's 
maturity. Reaching stability for the majority of our published production modules
is a stated project goal.

As a component of OpenTelemetry, we will follow the strategy outlined
in the 
[Versioning and Stability](https://opentelemetry.io/docs/specs/otel/versioning-and-stability/)
section in the specification. 

### API Stability

Stable APIs provide a guarantee against breaking changes across minor version updates.
This API stability helps with developer adoption. Breaking changes to stable APIs may
only happen when there is a major version bump.

The following modules are already marked as API stable:

* `agent-api`
* `android-agent`
* `android-instrumenation` (api)
* `session`

For this roadmap, we also seek API stability for:

* `common`
* `core` - This is still TBD. Long term, we may want to work toward eliminating
  `core`, or make it internal only.
* `services`

Other modules not covered below will be out of scope and are not intended to 
reach a stable API.

### Instrumentation Stability

We consider instrumentation stability a mid-range roadmap goal.
Instrumentation stability is challenging, because it involves several overlapping
aspects, not including the (already stable) instrumentation API:

* Behavior and telemetry - This concerns the behavior of the instrumentation,
  when and how it applies, and the volume and shape of the telemetry generated. 
  A stable instrumentation is consistent in these aspects, and may not alter
  these without an opt-in configuration setting.
* Configurability - Instrumentations may have a configuration surface that
  allows certain behavioral aspects to be customized. A stable instrumentation 
  must maintain a consistent set of configurations across minor versions.  
* Semantic conventions - Stable instrumentations provide known, documented
  values for span names, event names, and attributes. Even if these names and 
  values themselves are not yet stable, a stable instrument will need to be 
  consistent in the values that it applies, unless an opt-in flag is used.
  Ideally, stable instrumentations should generate stable semantic conventions. 

In order for an instrumentation to be marked stable, all of the above should apply.
This should start following after API stability has been completed.

### Semantic convention stability

A longstanding goal of all OTel client SIGs (Android, iOS, and web) is to 
agree on common, reusable semantic conventions. Due to platform differences,
this is not always possible. Where it is possible, the conventions will live
in a separate repository.

For semantic conventions that are specific to Android, we use federated semantic
conventions. These conventions will follow the typical stability lifecycle, 
and we consider stabilization of these to be an OTel Android roadmap priority.

#### Semantics and metadata

As instrumentation matures and semantic conventions stabilize, we will create
a set of metadata to describe what this data looks like. This metadata will
be used to generate documentation.

## Agent Enhancements

The `android-agent` domain-specific language (DSL) is the primary API surface
through which most developers will configure OpenTelemetry Android. We are
committed to continuing to enhance this DSL in order to make it as 
user-friendly and feature-rich as practical for users and maintainers.

These efforts will expand to include broader instrumentation configuration,
as well as additional detailed configuration of the underlying OTel SDK.

## Android and Kotlin compatibility

This project seeks to stay current with development updates
from the upstream Android project and from the Kotlin language.
This is a stated project roadmap goal. Users who bootstrap a new 
Android project with the latest Android SDK and AGP should always
be able to use the latest version of OpenTelemetry Android without
jumping through many hoops.

Furthermore, users who are still on older, but still supported, versions
of the Android SDK / AGP should still be able to utilize OpenTelemetry
Android without considerable effort.

See
[VERSIONING.md](VERSIONING.md#android-ecosystem-compatibility)
for additional specifics.

What this means, in practice, is that we will likely introduce breaking 
major version bumps, approximately once every year. This allows us to 
stay up to date with Android and AGP versions while still not constantly
breaking users.

## Declarative Configuration

OpenTelemetry has designed a sophisticated yaml-based, declarative configuration 
specification that is robust, flexible, and extensible. This allows users to build
configuration tooling and to mix and match YAML snippets across applications to
configure their OpenTelemetry SDKs and instrumentation libraries.

OpenTelemetry Android is committed to maintaining and continuously improving our
`android-agent` DSL, but we must also support declarative configuration. Supporting
declarative configuration in OpenTelemetry Android is a roadmap priority. We will
find ways of doing this to remain interoperable with the agent DSL.

We believe that supporting declarative configuration may also be a way to
keep our API surface small, potentially shrinking it long term.

## Documentation

As OpenTelemetry matures, our documentation will mature along with it.

The majority of developer-facing documentation will live in the
[opentelemetry.io repo](https://github.com/open-telemetry/opentelemetry.io)
and published to the 
[opentelemetry.io website](https://opentelemetry.io/docs/platforms/client-apps/android/). 

As noted above, we will work toward auto-generating instrumentation-specific
documentation from metadata. This is a long term goal.

We also want it to be easy for users to find what they are looking for. We think
that a set of common useful techniques can help users. This will be written in 
the form of a recipe book consisting of a series of "How do I ..." articles 
for common practical scenarios.

## Instrumentation Enhancements

We believe that there is plenty of room to enhance OpenTelemetry Android with
additional instrumentation libraries. While this is somewhat open-ended, we 
welcome pragmatic contributions for new frameworks and libraries, especially
those used widely by Android developers.

<tbd>

---

add additional instrumentation
use pure kotlin upstream (not java)
move demo to community
grow approvers
accelerate contributions
android platform and kotlin language updates(?)

long term future
- what's in 2.x?
- more build-time integration/injection
- session replay
- integration with webviews/react native/etc.
