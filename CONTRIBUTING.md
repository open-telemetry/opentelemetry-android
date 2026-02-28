# Contributing

Welcome to OpenTelemetry Android repository!

Before you start - see OpenTelemetry general
[contributor guide](https://github.com/open-telemetry/community/tree/main/guides/contributor)
requirements and recommendations.

Make sure to review the projects [license](LICENSE) and sign the
[CNCF CLA](https://identity.linuxfoundation.org/projects/cncf). A signed CLA will be enforced by an
automatic check once you submit a PR, but you can also sign it after opening your PR.

## Requirements

Java 17 or higher is required to build the projects in this repository.
The built artifacts can be used with Android API Level 21 and higher.
API levels 21 to 25 require desugaring of the core library.

## Building opentelemetry-android

1. Clone the repository

```
git clone https://github.com/open-telemetry/opentelemetry-android.git
cd opentelemetry-android
```

2. To build the android artifact, run the gradle wrapper with `assemble`:

```
./gradlew assemble
```

The output artifacts will be in `instrumentation/build/outputs/`.

3. To run the tests and code checks:

```
./gradlew check
```

## Submitting Pull Requests (PRs)

Pull requests are welcome! As an open source community project, OpenTelemetry Android
relies on code submissions from its contributors. There are a few things to consider
before submitting a pull request.

1. Before starting work, check
   [the open issues list](https://github.com/open-telemetry/opentelemetry-android/issues)
   to see if an issue already exists. If it does, you may comment on the issue and
   ask to be assigned. Assignments communicate to other contributors that the work
   has already been started and is in progress.
2. Issues are NOT required for every PR. You may readily submit a PR without an issue.
3. Keep your PRs small! This cannot be emphasized enough. There is no formal upper bound
   on size, but PRs that are thousands of lines long take a very long time and lots
   of effort to review. Find ways of decomposing the work into smaller units to keep the
   size of your PRs down. Incremental changes are favored over widespread/far-reaching
   refactors.
4. Keep your PRs single-purpose! This is subjective, but PRs should usually have a single
   purpose/idea/goal and make one clear change. Contributors should avoid making unrelated
   changes to separate code areas in the same PR. Keeping PRs single-purpose will also
   help to keep them small.
5. If an issue exists, mention it in the PR description. If the PR is the final effort
   for a given issue, please add `Resolves #nnn` (where nnn is the issue number) somewhere
   in the PR description, so that the issue can be automatically closed when the PR is
   merged. This also leaves a nice audit trail for future developers.

### Guidelines for merging pull requests

Maintainers have the permissions to merge pull requests. The process for merging pull requests after
review approval differs depending on its complexity.

For example, a simple Renovate/dependency bump PR probably only requires approval from one
approver/maintainer.

A complex change with a wide scope probably requires approval from more than one
maintainer/approver. If a change alters the public API, it's also worth waiting a couple of days
to give folks a chance to provide their opinions. Discussing API changes in the SIG is always
welcome.

## Code Conventions

We use [spotless](https://github.com/diffplug/spotless) to enforce a consistent code style
throughout the project. This includes reformatting (linting) of both source code and markdown.

Before submitting a PR, you should ensure that your code is linted. We use the
[spotless gradle plugin](https://github.com/diffplug/spotless/tree/main/plugin-gradle) to
make this easy. You should run it like this:

```
./gradlew spotlessApply
```

### Tests

#### Framework

By default we use JUnit 5, with some exceptions:

- When writing [Android tests](https://developer.android.com/training/testing/instrumented-tests).
- When writing [Robolectric tests](https://robolectric.org/).

For both, Android and Robolectric tests, we use JUnit 4 as they currently don't support JUnit 5.

#### Assertions

This project has standardized on
[AssertJ](https://joel-costigliola.github.io/assertj/)
for fluent test assertions, rather than the default JUnit assertions. Please
use AssertJ when writing tests. For example, instead of `assertEquals(that, thiz)`
you should write `assertThat(thiz).isEqualTo(that)`.

For clarity, assert methods should be brought in via static import.

#### Mocks

OpenTelemetry Android has standardized on
[MockK](https://mockk.io/) as the preferred Kotlin mocking framework for tests.
When writing test code, please use MockK instead of Mockito.

#### Instrumentation tests

For instrumentations that require bytecode weaving we create a test application
with [Android tests](https://developer.android.com/training/testing/instrumented-tests) as those are
the only kind of tests that
support library bytecode weaving. Ideally we should be able to validate bytecode weaving only by
creating tests using Robolectric, but that's not supported for now (for more info on the matter take
a look at [this google issue](https://issuetracker.google.com/issues/249940660) about it).

The test application module should be placed in the same directory as the instrumentation agent and
library modules and should be named `testing`, as shown below:

```text
instrumentation/
├─ my-instrumentation/
│  ├─ agent/
│  ├─ library/
│  ├─ testing/
```

You can take a look at how it's done for the OkHttp
instrumentation [here](instrumentation/okhttp) for reference.

## API Compatibility

This project leverages the kotlin
[binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator)
to detect and make explicit/intentional any changes to the public api surface. If you have
made changes locally and wish to see if there are api changes, you can use the following:

```
$ ./gradlew apiCheck
```

Note that `apiCheck` is run as part of the `check` task as well, so if there are unresolved
api changes, the `check` will fail.

If you have an intentional changes, you can regenerate the api file(s) with the following:

```
$ ./gradlew apiDump
```
