plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android sessions instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.sessions"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(project(":instrumentation:android-instrumentation"))
//    api(platform(libs.opentelemetry.platform.alpha))
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.semconv.incubating)
    api(project(":core"))
    api(project(":common"))
    api(project(":session"))

//    implementation(project(":services"))
//    implementation(project(":common"))
//    api(platform(libs.opentelemetry.platform.alpha))
//    api(libs.opentelemetry.api)
//    implementation(libs.androidx.core)
//    implementation(libs.opentelemetry.semconv)
//    implementation(libs.opentelemetry.sdk)
//    implementation(libs.opentelemetry.instrumentation.api)
//    testImplementation(libs.robolectric)
}
