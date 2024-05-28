import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("otel.errorprone-conventions")
}

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val minKotlinVersion = rootProject.extra["kotlin_min_supported_version"] as KotlinVersion
android {
    namespace = "io.opentelemetry.android"
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
        apiVersion = minKotlinVersion.version
        languageVersion = minKotlinVersion.version
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    androidTestImplementation(libs.findLibrary("androidx-test-runner").get())
    androidTestImplementation(libs.findLibrary("opentelemetry-sdk-testing").get())
    coreLibraryDesugaring(libs.findLibrary("desugarJdkLibs").get())
}