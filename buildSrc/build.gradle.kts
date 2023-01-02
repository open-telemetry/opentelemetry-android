plugins {
    `kotlin-dsl`

    // When updating, update below in dependencies too
    id("com.diffplug.spotless") version "6.12.1"
}

spotless {
    kotlinGradle {
        ktlint()
        target("* * / *.gradle.kts")
    }
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    // keep this version in sync with /build.gradle.kts
    implementation("com.android.tools.build:gradle:7.3.1")

    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.12.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.0.1")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin:1.5.0")
}
