plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

android {
    namespace = "io.opentelemetry.android.agent"
}

dependencies {
    api(project(":core"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.instrumentation.api)
    implementation(project(":common"))
    implementation(project(":session"))
    implementation(project(":services"))
    implementation(libs.opentelemetry.exporter.otlp)

    // Default instrumentations:
    api(project(":instrumentation:activity"))
    api(project(":instrumentation:anr"))
    api(project(":instrumentation:crash"))
    api(project(":instrumentation:fragment"))
    api(project(":instrumentation:network"))
    api(project(":instrumentation:slowrendering"))
    api(project(":instrumentation:startup"))

    testImplementation(libs.robolectric)
}

extra["pomName"] = "OpenTelemetry Android Agent"
description =
    "A library that contains all the commonly needed instrumentation for Android apps in a " +
    "convenient way with minimum configuration needed."
