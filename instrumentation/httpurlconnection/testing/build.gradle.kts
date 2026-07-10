plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.opentelemetry.android.httpurlconnection.test"
}

dependencies {
    byteBuddy(project(":instrumentation:httpurlconnection:agent"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":instrumentation:httpurlconnection:library"))
    implementation(project(":test-common"))
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.auto.service.annotations)
    ksp(libs.auto.service.processor)
    androidTestImplementation(project(":core"))
    androidTestImplementation(libs.assertj.core)
}
