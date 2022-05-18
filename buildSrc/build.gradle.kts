plugins {
    `kotlin-dsl`

    // When updating, update below in dependencies too
    id("com.diffplug.spotless") version "6.4.2"
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
    implementation("com.android.tools.build:gradle:7.2.0")

    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.4.2")
}
