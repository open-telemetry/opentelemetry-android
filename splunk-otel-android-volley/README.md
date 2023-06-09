
# Splunk OpenTelemetry Android Volley Instrumentation

> :construction: &nbsp;Status: Experimental

This directory contains the Splunk instrumentation for the [Volley](https://google.github.io/volley/)
HTTP client library. If you use the Volley HTTP client in your Android application, you can
leverage this instrumentation to help simplify tracing and capture of HTTP headers.

## Add to your project

In addition to the [main Splunk Android SDK dependency](https://github.com/signalfx/splunk-otel-android#getting-the-library),
you will also need to add the `splunk-otel-android-volley` dependency to your `build.gradle.kts`:


```gradle
dependencies {
    ...
    implementation("com.splunk:splunk-otel-android-volley:1.0.0")
    ...
}
```

## How to use VolleyTracing

See the class `VolleyExample.java` file in the sample application in this repository for an
example of how to create an instrumented client HTTP call.

First, you will create an instance of `VolleyTracing` by passing your `splunkRum` instance
to the builder:

```java
VolleyTracing volleyTracing = VolleyTracing.builder(splunkRum).build();
```
You can also add extra request/response headers to capture to the builder.
These headers will be attached to the RUM Volley HTTP client spans with
`http.request.header.` or `http.response.header.` respectively.

Next, get an instance of `HurlStack` from your `volleyTracing` instance:

```java
HurlStack hurlStack = volleyTracing.newHurlStack();
```

Then, use the `hurlStack` to create your request queue and send requests like you normally would.

# Troubleshooting

This library is `Experimental` and we actively welcome feedback. Please
[open an issue](https://github.com/signalfx/splunk-otel-android/issues/new) to report any
challenges or concerns.
