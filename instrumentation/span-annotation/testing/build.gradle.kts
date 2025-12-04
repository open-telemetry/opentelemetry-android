plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android.namespace = "io.opentelemetry.android.spanannotation"

dependencies {
    byteBuddy(project(":instrumentation:span-annotation:agent"))
    implementation(project(":instrumentation:span-annotation:library"))
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(project(":test-common"))
}
