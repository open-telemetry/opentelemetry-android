# Releasing

This documents describes how to publish a release to maven central, either to the release path or
the snapshot one.

These are the steps to follow:

- Make sure that the `gradle.properties` version property is set to the value you want to release.
- Go to
  the [release action](https://github.com/open-telemetry/opentelemetry-android/actions/workflows/release.yml)
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