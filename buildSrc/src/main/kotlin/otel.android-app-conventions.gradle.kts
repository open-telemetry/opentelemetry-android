import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.application")
    id("otel.errorprone-conventions")
}

android {
    namespace = "io.opentelemetry.android"
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        val javaVersion = rootProject.extra["java_version"] as JavaVersion
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }
}

dependencies {
    implementation(platform(project(":dependencyManagement")))

    androidTestImplementation("androidx.test:runner:${rootProject.extra["androidTestRunnerVersion"]}")
    androidTestImplementation("io.opentelemetry:opentelemetry-sdk-testing:${rootProject.extra["otelSdkVersion"]}")
}