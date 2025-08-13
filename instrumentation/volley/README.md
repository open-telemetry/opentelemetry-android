
# OpenTelemetry Android Volley Instrumentation

> :warning: &nbsp;Status: DEPRECATED

This instrumentation is deprecated and will be removed in a future release.
It is not recommended to use this instrumentation in new deployments.

This directory contains OpenTelemetry instrumentation for the [Volley](https://google.github.io/volley/)
HTTP client library. If you use the Volley HTTP client in your Android application, you can
leverage this instrumentation to help simplify tracing and capture of HTTP headers.

## Add to your project

To use this library instrumentation, you will need to add
the dependency in your `build.gradle.kts`:

```gradle
dependencies {
    ...
    implementation("io.opentelemetry.android.instrumentation:volley:{version}")
    ...
}
```

## How to use VolleyTracing

To use `VolleyTracing`, you must have already initialized an `OpenTelemetry` instance.
To build an instrumented client HTTP call, you will first create an instance of
`VolleyTracing` by passing in the `OpenTelemetry` instance to the builder:

```java
VolleyTracing volleyTracing = VolleyTracing.builder(openTelemetry).build();
```

If you're using `OpenTelemetryRum`, you can get the `OpenTelemetry` instance from it:

```java
VolleyTracing volleyTracing = VolleyTracing.builder(otelRum.getOpenTelemtry()).build();
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
[open an issue](https://github.com/open-telemetry/opentelemetry-android/issues) to report any
challenges or concerns.
