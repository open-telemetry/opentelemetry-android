plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android View clcik library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.view.click.library"
}

dependencies {
    api(project(":services"))
    api(libs.opentelemetry.api)
    api(platform(libs.opentelemetry.platform.alpha))
    api(project(":instrumentation:android-instrumentation"))

    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.api.incubator)

    testImplementation(project(":test-common"))
    testImplementation(project(":session"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
