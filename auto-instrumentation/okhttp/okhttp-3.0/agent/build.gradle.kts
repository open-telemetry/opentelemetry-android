plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation(project(":auto-instrumentation:okhttp:okhttp-3.0:library"))
    implementation("net.bytebuddy:byte-buddy:${property("bytebuddy.version")}")
}
