import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.FileInputStream
import java.util.Properties

plugins {
    // NOTE: We specifically do NOT use the "otel.android-app-conventions" here
    // This is due to strict version requirements between the compose compiler and
    // the gradle kotlin plugin.
    // See https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("otel.errorprone-conventions")
}

val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val minKotlinVersion = rootProject.extra["kotlin_min_supported_version"] as KotlinVersion

android {
    namespace = "io.opentelemetry.android.demo"
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        applicationId = "io.opentelemetry.android.demo"
        minSdk = (property("android.minSdk") as String).toInt()
        targetSdk = (property("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildTypes {
        all {
            val accessToken = localProperties["rum.access.token"] as String?
            resValue("string", "rum_access_token", accessToken ?: "fakebroken")
        }
        release {
            //TODO: Get minification working one day for compatibility testing
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
}

dependencies {
    // Required to be kept in sync with the compatible version of jetpack compose
//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(project(":android-agent"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)
    testImplementation(libs.bundles.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.opentelemetry.sdk.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
