# Repository settings

This document describes any changes that have been made to the
settings for this repository beyond the [OpenTelemetry default repository
settings](https://github.com/open-telemetry/community/blob/main/docs/how-to-configure-new-repository.md#repository-settings).

## Branch protections

### `main`

- Require branches to be up to date before merging: UNCHECKED
  (PR jobs take too long, and leaving this unchecked has not been a significant problem)


## Secrets and variables > Actions

* `GPG_PASSWORD` - stored in OpenTelemetry-Java 1Password
* `GPG_PRIVATE_KEY` - stored in OpenTelemetry-Java 1Password
* `SONATYPE_KEY` - owned by [@breedx-splk](https://github.com/breedx-splk)
* `SONATYPE_USER` - owned by [@breedx-splk](https://github.com/breedx-splk)