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
    * [Semantics and metadata](#semantics-and-metadata)
* [Agent Enhancements](#agent-enhancements)
* [Android and Kotlin compatibility](#android-and-kotlin-compatibility)
* [Declarative Configuration](#declarative-configuration)
* [Documentation](#documentation)
* [Instrumentation Enhancements](#instrumentation-enhancements)
* [Growing Contributors](#growing-contributors)
* [Distant Destinations](#distant-destinations)
  * [Pure Kotlin-based Upstream](#pure-kotlin-based-upstream)
  * [Upstream the Demo App](#upstream-the-demo-app)
  * [Session Replay](#session-replay)
  * [Additional Native Integrations](#additional-native-integrations)
* [Conclusion](#conclusion)

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

A comprehensive list of future instrumentation modules is not listed here, and
so it remains open-ended.

We will persue additional build-time auto-instrumentation. This helps with 
our roadmap goal of allowing developers to add OpenTelemetry to their projects
without having to write much code, especially code that exists in production
modules. It is expected that additional build-time instrumentation will be
built as gradle plugins.

## Growing Contributors

The long-term success of OpenTelemetry Android depends on having a robust
pipeline of maintainers, approvers, code contributors, and reviewers. We 
will continue to support and grow our contributor base to help ensure
that we have consistent, robust coverage in these areas.

## Distant Destinations

It's probably worth noting down briefly other ideas that have surfaced. 
We expect these to be larger or slower/longer efforts that may not land
in the next 1-2 years, but we're also open to reprioritizing as things 
change.

### Pure Kotlin-based Upstream

Once [opentelemetry-kotlin](https://github.com/open-telemetry/opentelemetry-kotlin)
has reached stable milestones for API and SDK, we can work on switching to
a different upstream. This allows us to use idiomatic kotlin more consistently
throughout OpenTelemetry Android, without the need to rely on legacy Java
syntax and Java interoperation. 

### Upstream the Demo App

We have a pretty feature-rich [demo-app](demo-app) embedded into this
repo. That's great for local development and for testing and demonstrating
features, but we also feel like it would be more useful as a first-class
component within the
[official OpenTelemetry Demo](https://github.com/open-telemetry/opentelemetry-demo).

There's some work that would need to be done first to get the demo app in 
a shape to be moved over, including building a non-mocked service layer.

The tracking issue for this work is in
[#1724](https://github.com/open-telemetry/opentelemetry-android/issues/1724).

### Session Replay

Advanced RUM system leverage "session replay" in order to play back a 
user's interaction with an application along a timeline. This allows
user experience (UX) researchers the ability to deeply understand
how users interact with a given design.

In addition to the technical work required to build this, other challenges
include obvious privacy concerns and the absence of an appropriate OTLP style
data model.

### Additional Native Integrations

Many Android applications leverage cross-platform stacks for some of their 
implementation, including embedded web views, React Native, and Flutter.
The current state of OpenTelemetry leaves an integration gap here that we 
should improve one-day. This would help with end-to-end observability.

Sharing the OpenTelemetry context between these stacks should be made
possible one day, and cross-stack instrumentation should be made 
installable and/or interoperable.

## Conclusion

This roadmap captures the areas where OpenTelemetry Android needs continued
investment in order to become more stable, easier to adopt, and more useful
for Android developers. Progress will depend on focused, incremental
contributions that improve stability, compatibility, documentation, and
instrumentation without expanding the project beyond what maintainers can
support.
