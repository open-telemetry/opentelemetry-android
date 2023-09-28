plugins {
    `kotlin-dsl`

    // When updating, update below in dependencies too
    id("com.diffplug.spotless") version "6.21.0"
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
    implementation("com.android.tools.build:gradle:8.1.2")

    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.21.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin:1.6.0")
    implementation("ru.vyarus:gradle-animalsniffer-plugin:1.7.1")
}
