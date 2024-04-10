# Releasing

This documents describes the manual steps required to publish a release to maven central.

These are the steps to follow:

- Review the recent [list of open PRs](https://github.com/open-telemetry/opentelemetry-android/pulls) 
  to determine if any need to be merged before cutting a release.
- Make sure that the `gradle.properties` version property is set to the value you want to release. 
  This must be different than the most recent release number (typically one minor version increase).
- Merge a pull request to `main` branch that updates the `CHANGELOG.md`.
  - The heading for the unreleased entries should be `## Unreleased`.
- Go to the 
  [prepare-release-branch action](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/prepare-release-branch.yml)
  in Github and click on "Run workflow".
- You can choose the branch where you want to create a release from, as well as whether the release
  is "final" or a "snapshot" one.
- Run the release workflow with your chosen parameters.

If the version released was `final`:

- Create a PR to update the version in the `gradle.properties` file to
  the next development version. This PR can and probably should also include updating any
  documentation (CHANGELOG.md, README.md, etc) that mentions the previous version.
- Once this PR is merged, create a release in Github that points at the newly created version, and
  make sure to provide release notes that at least mirror the contents of the CHANGELOG.md

> Please note that the artifacts are published into maven central, which tends to have a delay of
> roughly half an hour, more or less, before making the newly published artifacts actually available
> for fetching them.
