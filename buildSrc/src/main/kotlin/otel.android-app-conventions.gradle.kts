import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.application")
    id("otel.errorprone-conventions")
}

android {
    namespace = "io.opentelemetry.android"
    compileSdk = 33

    defaultConfig {
        minSdk = (project.property("android.minSdk") as String).toInt()
    }

    compileOptions {
        val javaVersion = rootProject.extra["java_version"] as JavaVersion
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }
}