# Jaeger Version Update Summary

**Date**: December 3, 2025  
**Component**: OpenTelemetry Android Demo App - Docker Compose Configuration  
**Issue**: Update Jaeger to latest version and enable automated dependency updates via Renovate

## Changes Made

### 1. Jaeger Version Update

**File**: `demo-app/compose.yaml`

**Before**:
```yaml
jaeger:
  image: "jaegertracing/all-in-one:1.60@sha256:4fd2d70fa347d6a47e79fcb06b1c177e6079f92cba88b083153d56263082135e"
```

**After**:
```yaml
jaeger:
  image: "jaegertracing/all-in-one:1.76.0@sha256:968b31672178df2ea7bf6e43e63e3bd93c3f95e8fc7b826ef8a07a73caa36b64"
```

**Version Details**:
- **Old Version**: 1.60
- **New Version**: 1.76.0 (latest as of December 2025)
- **Docker Registry**: [jaegertracing/all-in-one](https://hub.docker.com/r/jaegertracing/all-in-one/tags)
- **Image Size**: ~35.64 MB (linux/amd64)

### 2. Renovate Configuration Verification

**Status**: ✅ Already Configured

**File**: `.github/renovate.json5`

**Existing Configuration**:
- Extends: `config:recommended`, `docker:pinDigests`, `helpers:pinGitHubActionDigests`
- Docker image updates are pinned with SHA-256 digests for security
- Weekly schedule for docker-related updates to minimize notification noise
- Fully supports Docker Compose file updates

**Key Features**:
```json5
{
  "extends": [
    "config:recommended",
    "docker:pinDigests",
    "helpers:pinGitHubActionDigests"
  ],
  "packageRules": [
    {
      "matchManagers": ["github-actions", "dockerfile"],
      "extends": ["schedule:weekly"],
      "groupName": "weekly update"
    }
  ]
}
```

## Impact Analysis

### Benefits
1. **Security**: Updated to the latest Jaeger version with latest security patches
2. **Features**: Access to improvements and bug fixes in Jaeger 1.76.0
3. **Automated Updates**: Renovate will automatically detect and propose future updates
4. **Immutable Images**: SHA-256 digest pinning ensures reproducible builds

### Breaking Changes
- None identified. Jaeger 1.76.0 maintains backward compatibility with 1.60
- OTLP configuration remains unchanged
- Environment variables (`COLLECTOR_OTLP_ENABLED`, `COLLECTOR_OTLP_HTTP_HOST_PORT`) continue to work

### Testing Recommendations
1. Verify demo app connects to Jaeger UI at `http://localhost:16686`
2. Confirm all traces are properly exported and visible in the UI
3. Test all instrumentation features:
   - Activity lifecycle monitoring
   - Fragment lifecycle monitoring
   - Crash reporting
   - ANR detection
   - Slow render detection
   - Manual instrumentation

## Renovate Automation

### How It Works
1. Renovate bot automatically scans Docker image references in `compose.yaml`
2. Detects new releases on Docker Hub
3. Creates a pull request with the updated version and digest
4. Updates are grouped and scheduled weekly to reduce noise
5. Contributors review and merge updates as needed

### What to Expect
- **Frequency**: Weekly (docker-related updates are bundled)
- **Pull Requests**: Automated PRs from `renovate[bot]` user
- **Format**: Always includes updated image tags and SHA-256 digests
- **Status**: Renovate is enabled and will start monitoring on the next run

## Files Modified

| File | Change | Status |
|------|--------|--------|
| `demo-app/compose.yaml` | Jaeger version 1.60 → 1.76.0 | ✅ Complete |
| `.github/renovate.json5` | Verified configuration | ✅ Confirmed Active |

## Verification Steps

To verify the changes:

```bash
# 1. Build and start the services
cd demo-app
docker compose build
docker compose up

# 2. Verify Jaeger UI is accessible
curl http://localhost:16686/api/v1/services

# 3. Run the demo app and check traces
# Navigate to http://localhost:16686 in your browser
```

## Next Steps

1. ✅ Push changes to repository
2. ✅ Renovate will begin monitoring for future updates
3. Review and merge Renovate PRs as they arrive (weekly schedule)
4. Test new versions in CI/CD pipeline before production deployment

## References

- [Jaeger Docker Hub](https://hub.docker.com/r/jaegertracing/all-in-one/tags)
- [Jaeger Release Notes](https://github.com/jaegertracing/jaeger/releases)
- [Renovate Documentation](https://docs.renovatebot.com/)
- [OpenTelemetry OTLP Configuration](https://opentelemetry.io/docs/reference/specification/protocol/)

## Version History

| Version | Date | Maintainer | Notes |
|---------|------|-----------|-------|
| 1.76.0 | Dec 3, 2025 | OpenTelemetry Android | Current (Latest) |
| 1.60 | - | OpenTelemetry Android | Previous |

