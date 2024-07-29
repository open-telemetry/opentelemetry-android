plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

android {
    namespace = "io.opentelemetry.android.agent"
}

dependencies {
    api(project(":core"))

    // Default instrumentations:
    api(project(":instrumentation:activity"))
    api(project(":instrumentation:fragment"))
    api(project(":instrumentation:crash"))
    api(project(":instrumentation:startup"))
    api(project(":instrumentation:slowrendering"))
    api(project(":instrumentation:anr"))
    api(project(":instrumentation:network"))
}

extra["pomName"] = "OpenTelemetry Android Agent"
description =
    "A library that contains all the commonly needed instrumentation for Android apps in a convenient way for minimum configuration."
