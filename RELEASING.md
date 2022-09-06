# Release Process

## Releasing a new version

splunk-otel-android is released via a private Splunk gitlab installation.

This is the process to use to do a release:

1) Make sure that all the required changes are merged. This includes updating the upstream OTel
   libraries' versions, and making sure that the project version in the `gradle.properties` file is
   correctly set to the next planned release version.

2) Run the `scripts/tag-release.sh` script to create and push a signed release tag. Note that it
   assumes that the remote is named `origin`, if you named yours differently you might have to push
   the tag manually.

3) Wait for gitlab to run the release job.

4) Log in to `oss.sonatype.org` with a profile that has permissions to see and release the `com.splunk`
   staging repository.

5) Close the staging repository, and then release it via the oss.sonatype.org UI.
   Ideally, verify that publishing worked by pulling down the dependency in some sample project
   and build an application against it. This will double-check that it's a working release.

6) Create a PR to update the version in the `gradle.properties` to the next development
   version. This PR can and probably should also include updating any documentation (CHANGELOG.md,
   README.md, etc) that mentions the previous version.

7) Once this PR is merged, create a release in Github that points at the newly created version,
   and make sure to provide release notes that at least mirror the contents of the CHANGELOG.md
