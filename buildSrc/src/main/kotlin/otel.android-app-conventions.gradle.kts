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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.put(
            "runnerBuilder",
            "de.mannodermaus.junit5.AndroidJUnit5Builder"
        )
    }

    compileOptions {
        val javaVersion = rootProject.extra["java_version"] as JavaVersion
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }

    packaging {
        resources.excludes += "META-INF/LICENSE*.md"
    }
}

val androidXTestVersion = "1.5.0"
dependencies {
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")

    androidTestImplementation("de.mannodermaus.junit5:android-test-core:1.2.2")
    androidTestRuntimeOnly("de.mannodermaus.junit5:android-test-runner:1.2.2")
}