# Releasing

This documents describes how to publish a release to maven central, either to the release path or
the snapshot one.

## Credentials

The following environment variables are required for publishing:

* `GPG_PRIVATE_KEY` and `GPG_PASSWORD`: GPG private key and password for signing.
* `SONATYPE_USER` and `SONATYPE_KEY`: Sonatype username and password.

## Publishing a final release

In order to publish a final release, you must pass the `-Prelease=true` parameter to the gradle
command like so:

```sh
./gradlew assemble publishToSonatype closeAndReleaseSonatypeStagingRepository -Prelease=true
```

The `-Prelease=true` parameter will ensure that the project version doesn't get the "-SNAPSHOT"
suffix added which is what the [Nexus publish](https://github.com/gradle-nexus/publish-plugin)
plugin takes into account when deciding which path to send the artifacts to.

## Publishing a snapshot release

The version of the project will get the "-SNAPSHOT" suffix added by default, which is what's needed
to send the publication over to maven central's snapshot path. Therefore, the command to publish
a snapshot artifact will look like so:

```sh
./gradlew assemble publishToSonatype closeAndReleaseSonatypeStagingRepository
```

Unlike when publishing a final artifact, the snapshot publishing command doesn't need
the `-Prelease=true` parameter.
