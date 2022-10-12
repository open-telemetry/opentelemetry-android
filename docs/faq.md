
## Frequently Asked Questions (FAQ)

This document services as a central place for frequently asked
questions (FAQ) and common setup and troubleshooting advice.
Please [contribute](../CONTRIBUTING.md) to this list.

### Why do I see "error handling gzip compressed request EOF" in Android Studio when I include Splunk RUM in my app?

Splunk RUM uses gzip encoding to reduce network bandwidth consumption. Android Studio's
Network Inspector attempts to read the body of the http request and breaks the gzip
encoding; which results in 400 responses from our ingest servers.

As a workaround, you can disable Splunk RUM while profiling your app, or you can disable
gzip compression at the time of profiling your app.

This is a known issue with Android Studio's Network Inspector.
To learn more please review: https://issuetracker.google.com/issues/200699798 and https://issuetracker.google.com/issues/200852831

### How does this relate to OpenTelemetry

We have started the process of donating the majority of this SDK to OpenTelemetry.
When we are finished, this project will be a lightweight customization of an upstream/official
OpenTelemetry Android Client SDK to support real user monitoring (RUM).

You can following along with the OpenTelemetry client
[SIG notes](https://docs.google.com/document/d/16Vsdh-DM72AfMg_FIt9yT9ExEWF4A_vRbQ3jRNBe09w/edit#heading=h.yplevr950565)
or see `#otel-client-side-telemetry` in CNCF slack.
