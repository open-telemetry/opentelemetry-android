plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android {
    namespace = "io.opentelemetry.android.httpurlconnection.test"
}

dependencies {
    byteBuddy(project(":instrumentation:httpurlconnection:agent"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":instrumentation:httpurlconnection:library"))
    implementation(project(":test-common"))
    androidTestImplementation(libs.assertj.core)
}
