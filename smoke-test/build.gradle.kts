import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.test")
    id("otel.spotless-conventions")
}

android {
    namespace = "io.opentelemetry.android.smoketest"
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":smoke-test-app"

    buildTypes {
        create("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            testProguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility(rootProject.extra["java_version"] as JavaVersion)
        targetCompatibility(rootProject.extra["java_version"] as JavaVersion)
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(rootProject.extra["jvm_target"] as JvmTarget)
            apiVersion.set(rootProject.extra["kotlin_min_supported_version"] as KotlinVersion)
            languageVersion.set(rootProject.extra["kotlin_min_supported_version"] as KotlinVersion)
        }
    }

    packaging.resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
}

androidComponents {
    // The test must run against the target's minified release variant.
    beforeVariants(selector().withBuildType("debug")) {
        it.enable = false
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.rules)
    implementation(libs.androidx.test.runner)
    implementation(libs.opentelemetry.proto)
}
