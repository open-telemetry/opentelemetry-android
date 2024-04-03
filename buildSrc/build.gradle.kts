plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.plugin)
    implementation(libs.errorprone.plugin)
    implementation(libs.nullaway.plugin)
    implementation(libs.animalsniffer.plugin)
    implementation(libs.kotlin.plugin)
}
