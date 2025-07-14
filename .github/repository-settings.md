# Repository settings

This document describes any changes that have been made to the
settings for this repository beyond the [OpenTelemetry default repository
settings](https://github.com/open-telemetry/community/blob/main/docs/how-to-configure-new-repository.md#repository-settings).

## Branch protections

### `main`

- Require branches to be up to date before merging: UNCHECKED
  (PR jobs take too long, and leaving this unchecked has not been a significant problem)

### `release/*`

Same settings as above for `main`, except:

* Restrict pushes that create matching branches: UNCHECKED

  (So that otelbot can create release branches)

### `renovate/**/**`, and `otelbot/*`

* Require status checks to pass before merging: UNCHECKED

  (So that renovate PRs can be rebased)

* Restrict who can push to matching branches: UNCHECKED

  (So that bots can create PR branches in this repository)

* Allow force pushes > Everyone

  (So that renovate PRs can be rebased)

* Allow deletions: CHECKED

  (So that bot PR branches can be deleted)
 
## Secrets and variables > Actions

* `GPG_PASSWORD` - stored in OpenTelemetry-Java 1Password
* `GPG_PRIVATE_KEY` - stored in OpenTelemetry-Java 1Password
* `SONATYPE_KEY` - owned by [@breedx-splk](https://github.com/breedx-splk)
* `SONATYPE_USER` - owned by [@breedx-splk](https://github.com/breedx-splk)

Note: `SONATYPE_KEY` and `SONATYPE_USER` were regenerated and replaced
on June 6th, 2025 for the [Sonatype OSSRH migration](https://central.sonatype.org/news/20250326_ossrh_sunset/).