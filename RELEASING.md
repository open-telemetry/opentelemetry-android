# Release Process

## Releasing a new version

Releasing of splunk-otel-android is done via a private Splunk gitlab installation.

This is the process to use to do a release:

1) Use the _Github_ UI to create a tag for the version (by creating a pre-release, for example),
pointing at the commit SHA on the main branch that you want to release.
The format of the tag should be "vx.y.z" or it won't be picked
up by gitlab as a release tag. You can also create a tag via the command line and push it to github
if you are more comfortable with that process.

2) Wait for gitlab to run the release job.

3) Log in to oss.sonatype.org with a login that has permissions to see and release the `com.splunk`
staging repository.

4) Close the staging repository, and then release it via the oss.sonatype.org UI.
Ideally, verify that the publishing worked by pulling down the dependency in some sample project and
build an application against it, as a double-check that it is a working release.

5) Create a PR that will update the version in the `gradle.properties` to the next one that will be being worked on.
This PR can and probably should also include updating any documentation (CHANGELOG.md, README.md, etc)
that mentions the previous version.

6) Once this PR is merged, create a release in Github that points at the newly created version, make sure
to provide release notes that at least mirror the contents of the CHANGELOG.md
