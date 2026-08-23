import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version libs.versions.minSupportedAgp
    id("org.jetbrains.kotlin.android") version libs.versions.minSupportedKotlin
}

val pins = gradle.startParameter.projectProperties

android {
    namespace = "io.opentelemetry.android.minversions.consumer"
    compileSdk = pins.getValue("minCompileSdk").toInt()

    defaultConfig {
        applicationId = "io.opentelemetry.android.minversions.consumer"
        minSdk = pins.getValue("minSdk").toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(platform("io.opentelemetry.android:opentelemetry-android-bom:${pins.getValue("bomVersion")}"))

    implementation("io.opentelemetry.android:android-agent")

    implementation("io.opentelemetry.android.instrumentation:android-log-library")
    implementation("io.opentelemetry.android.instrumentation:compose-click")
    implementation("io.opentelemetry.android.instrumentation:compose-navigation")
    implementation("io.opentelemetry.android.instrumentation:coroutines-library")
    implementation("io.opentelemetry.android.instrumentation:httpurlconnection-library")
    implementation("io.opentelemetry.android.instrumentation:native-crash")
    implementation("io.opentelemetry.android.instrumentation:okhttp3-library")
    implementation("io.opentelemetry.android.instrumentation:okhttp3-websocket-library")
    implementation("io.opentelemetry.android.instrumentation:power-save-mode")
    implementation("io.opentelemetry.android.instrumentation:thermal")
    implementation("io.opentelemetry.android.instrumentation:view-click")

    coreLibraryDesugaring(libs.desugarJdkLibs)
}
