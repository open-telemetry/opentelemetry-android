plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

dependencies {
    implementation(project(":auto-instrumentation:okhttp:okhttp-3.0:library"))
    implementation(libs.okhttp)
    implementation(libs.byteBuddy)
}
