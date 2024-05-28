plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
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
    implementation(libs.android.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.errorprone.plugin)
    implementation(libs.nullaway.plugin)
    implementation(libs.animalsniffer.plugin)
    implementation(libs.kotlin.plugin)
}
