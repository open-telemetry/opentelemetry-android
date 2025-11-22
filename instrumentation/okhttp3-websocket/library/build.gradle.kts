plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry OkHttp Websocket library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.okhttp.websocket.library"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":agent-api"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(project(":instrumentation:android-instrumentation"))
    implementation(project(":core"))
    implementation(project(":session"))
    compileOnly(libs.okhttp)
    api(libs.opentelemetry.instrumentation.okhttp)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.opentelemetry.sdk)
}
