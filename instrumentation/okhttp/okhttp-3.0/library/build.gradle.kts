plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry OkHttp library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.okhttp.library"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(libs.opentelemetry.instrumentation.okhttp)
    api(project(":android-agent"))
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    compileOnly(libs.okhttp)
}
