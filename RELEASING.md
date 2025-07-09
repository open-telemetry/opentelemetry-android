# Releasing

This documents describes the manual steps required to publish a release to maven central.

## Release cadence

This repository targets monthly minor releases from the `main` branch roughly a week after
the monthly minor release
of [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)).

## Prepare the release

We first need to prepare the release. This creates a versioned release branch, These are the steps to follow:

- Review the recent [list of open PRs](https://github.com/open-telemetry/opentelemetry-android/pulls)
  to determine if any need to be merged before cutting a release.
- Make sure that the `gradle.properties` version property is set to the value you want to release.
  This must be different than the most recent release number (typically one minor version increase).
- Merge a pull request to `main` branch that updates the `CHANGELOG.md`.
    - The heading for the unreleased entries must be `## Unreleased`.
    - Use [this action](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/draft-change-log-entries.yaml) as a starting point for writing the change log entries. It will print a draft in the console that you can copy to create your PR.
- Go to the
  [prepare-release-branch action](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/prepare-release-branch.yml)
  in Github and click on "Run workflow". This creates the release branch and does some prep.
- After the workflow finishes, it will have created 2 PRs -- one against `main` branch and
  one against the release branch. Review and merge these two PRs before running the release
  job (below).

## Run the release

Ensure that the preparation PR (created above) has been first merged into the release branch.

- The "prepare" step above should have created a PR that updates the version number in
  `gradle.properties`. This PR must be approved and merged before the release workflow is started,
  otherwise the release job will fail (the process explicitly checks for the version in the
  CHANGELOG.md). Because the release workflow runs against a release branch, it is safe to
  merge the `gradle.properties` into `main`.
- Run the [Release workflow](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/release.yml).
  - Press the "Run workflow" button, then select the release branch from the dropdown list,
    e.g. `release/v0.6.x`, and click the "Run workflow" button below that.
  - This workflow will publish the artifacts to maven central and will publish a GitHub release.
    The release will have release notes based on the CHANGELOG and will include `.zip` and
    `.tar.gz` bundles of the source code.

> Please note that the artifacts are published into maven central, which tends to have a delay of
> roughly half an hour, more or less, before making the newly published artifacts actually available
> for fetching them.
