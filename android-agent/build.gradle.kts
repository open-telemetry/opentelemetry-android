plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

android {
    namespace = "io.opentelemetry.android.agent"
}

dependencies {
    api(project(":agent-api"))
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":session"))
    implementation(project(":services"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":instrumentation:common-api"))
    implementation(libs.opentelemetry.exporter.otlp)

    // Default instrumentations:
    implementation(project(":instrumentation:activity"))
    implementation(project(":instrumentation:anr"))
    implementation(project(":instrumentation:crash"))
    implementation(project(":instrumentation:fragment"))
    implementation(project(":instrumentation:network"))
    implementation(project(":instrumentation:slowrendering"))
    implementation(project(":instrumentation:startup"))
    implementation(project(":instrumentation:sessions"))
    implementation(project(":instrumentation:screen-orientation"))

    testImplementation(libs.robolectric)
}

extra["pomName"] = "OpenTelemetry Android Agent"
description =
    "A library that contains all the commonly needed instrumentation for Android apps in a " +
    "convenient way with minimum configuration needed."
