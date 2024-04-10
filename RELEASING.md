# Releasing

This documents describes the manual steps required to publish a release to maven central.

## Prepare the release

We first need to prepare the release. This creates a versioned release branch, These are the steps to follow:

- Review the recent [list of open PRs](https://github.com/open-telemetry/opentelemetry-android/pulls) 
  to determine if any need to be merged before cutting a release.
- Make sure that the `gradle.properties` version property is set to the value you want to release. 
  This must be different than the most recent release number (typically one minor version increase).
- Merge a pull request to `main` branch that updates the `CHANGELOG.md`.
  - The heading for the unreleased entries must be `## Unreleased`.
- Go to the 
  [prepare-release-branch action](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/prepare-release-branch.yml)
  in Github and click on "Run workflow".
- After the workflow runs, review the resulting release PR and merge it. 

## Run the release

Ensure that the preparation PR (created above) has been first merged into the release branch.

- Run the [Release workflow](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/release.yml).
  - Press the "Run workflow" button, then select the release branch from the dropdown list,
    e.g. `release/v0.6.x`, and click the "Run workflow" button below that.
  - This workflow will publish the artifacts to maven central and will publish a GitHub release
    with release notes based on the change log and with the javaagent jar attached.
- The "prepare" step above should have created a PR that updates the version number in 
  `gradle.properties`. Once the release is complete, approve and merge that PR.

> Please note that the artifacts are published into maven central, which tends to have a delay of
> roughly half an hour, more or less, before making the newly published artifacts actually available
> for fetching them.
