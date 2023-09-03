plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

extra["artifactId"] = "okhttp-3.0-agent"

dependencies {
    implementation(project(":auto-instrumentation:okhttp:okhttp-3.0:library"))
    implementation("net.bytebuddy:byte-buddy:1.12.22")
}
