plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

dependencies {
    byteBuddy(project(":instrumentation:httpurlconnection:agent"))
    implementation(project(":instrumentation:httpurlconnection:library"))
    implementation(project(":test-common"))
    androidTestImplementation(libs.assertj.core)
}
