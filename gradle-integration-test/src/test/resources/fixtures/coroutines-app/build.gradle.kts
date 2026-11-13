import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The coroutines instrumentation needs a newer Kotlin than the other published artifacts, so it
// gets its own fixture instead of raising the minimum the other fixtures build with.
plugins {
    id("com.android.application") version libs.versions.minSupportedAgp
    id("org.jetbrains.kotlin.android") version libs.versions.minSupportedKotlinCoroutines
    id("net.bytebuddy.byte-buddy-gradle-plugin") version libs.versions.byteBuddy
}

val pins = gradle.startParameter.projectProperties

android {
    namespace = "io.opentelemetry.android.minversions.coroutines"
    compileSdk = pins.getValue("minCompileSdk").toInt()

    defaultConfig {
        applicationId = "io.opentelemetry.android.minversions.coroutines"
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
    implementation("io.opentelemetry.android.instrumentation:coroutines-library")
    implementation(libs.kotlinx.coroutines)

    byteBuddy(platform("io.opentelemetry.android:opentelemetry-android-bom:${pins.getValue("bomVersion")}"))
    byteBuddy("io.opentelemetry.android.instrumentation:coroutines-agent")

    coreLibraryDesugaring(libs.desugarJdkLibs)
}
