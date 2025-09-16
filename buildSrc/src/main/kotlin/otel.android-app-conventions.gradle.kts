import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("otel.errorprone-conventions")
    id("otel.android-dependency-conventions")
    id("otel.spotless-conventions")
}

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val targetJvm = rootProject.extra["jvm_target"] as JvmTarget
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

    kotlin {
        compilerOptions {
            jvmTarget.set(targetJvm)
            apiVersion.set(minKotlinVersion)
            languageVersion.set(minKotlinVersion)
        }
    }

    packaging {
        resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    androidTestImplementation(libs.findLibrary("androidx-test-core").get())
    androidTestImplementation(libs.findLibrary("androidx-test-rules").get())
    androidTestImplementation(libs.findLibrary("androidx-test-runner").get())
    androidTestImplementation(libs.findLibrary("opentelemetry-sdk-testing").get())
    coreLibraryDesugaring(libs.findLibrary("desugarJdkLibs").get())
}