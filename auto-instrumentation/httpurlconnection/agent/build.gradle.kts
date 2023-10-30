plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

dependencies {
    implementation(project(":auto-instrumentation:httpurlconnection:library"))
    implementation(libs.byteBuddy)
}
