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
instrumentation [here](instrumentation/okhttp3) for reference.

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
