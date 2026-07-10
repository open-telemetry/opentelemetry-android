# OpenTelemetry Android Roadmap

This document outlines the roadmap for OpenTelemetry Android. It describes the 
categorical topics of interest and the areas were development should focus.

This roadmap does not contain timelines or due dates. The speed of project
advancement is primarily limited by contributor availability.

Table of Contents:

* [Stability](#stability)
  * [API Stability](#api-stability)
  * [Instrumentation Stability](#instrumentation-stability)
  * [Semantic Convention Stability](#semantic-convention-stability)

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
* `core`
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

### Semantic convention stability

A longstanding goal of all OTel client SIGs (Android, iOS, and web) is to 
agree on common, reusable semantic conventions. Due to platform differences,
this is not always possible. Where it is possible, the conventions will live
in a separate repository.

For semantic conventions that are specific to Android, we use federated semantic
conventions. These conventions will follow the typical stability lifecycle, 
and we consider stabilization of these to be an OTel Android roadmap priority.

## Android and Kotlin compatibility

tbd

---

add additional instrumentation
use pure kotlin upstream (not java)
move demo to community
grow approvers
accelerate contributions
android platform and kotlin language updates(?)
declarative config and agent dsl enhancements
documentation and recipes

long term future
- what's in 2.x?
- more build-time integration/injection
- session replay
- integration with webviews/react native/etc.
