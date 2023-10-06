plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

dependencies {
    byteBuddy(project(":auto-instrumentation:okhttp:okhttp-3.0:agent"))
    implementation(project(":auto-instrumentation:okhttp:okhttp-3.0:agent"))
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
}
