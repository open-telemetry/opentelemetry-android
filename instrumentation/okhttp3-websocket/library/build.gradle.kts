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
    implementation(project(":instrumentation:android-instrumentation"))
    compileOnly(libs.okhttp)
    api(libs.opentelemetry.instrumentation.okhttp)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.api.incubator)
}
