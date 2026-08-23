import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Bytecode weaving runs the Byte Buddy Gradle plugin inside the consumer's own build, so it gets
// its own fixture.
plugins {
    id("com.android.application") version libs.versions.minSupportedAgp
    id("org.jetbrains.kotlin.android") version libs.versions.minSupportedKotlin
    id("net.bytebuddy.byte-buddy-gradle-plugin") version libs.versions.byteBuddy
}

val pins = gradle.startParameter.projectProperties

android {
    namespace = "io.opentelemetry.android.minversions.weaving"
    compileSdk = pins.getValue("minCompileSdk").toInt()

    defaultConfig {
        applicationId = "io.opentelemetry.android.minversions.weaving"
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
    implementation("io.opentelemetry.android.instrumentation:coroutines-library")
    implementation("io.opentelemetry.android.instrumentation:httpurlconnection-library")
    implementation("io.opentelemetry.android.instrumentation:okhttp3-library")
    implementation("io.opentelemetry.android.instrumentation:okhttp3-websocket-library")
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines)

    byteBuddy(platform("io.opentelemetry.android:opentelemetry-android-bom:${pins.getValue("bomVersion")}"))
    byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-agent")
    byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-websocket-agent")
    byteBuddy("io.opentelemetry.android.instrumentation:httpurlconnection-agent")
    byteBuddy("io.opentelemetry.android.instrumentation:android-log-agent")
    byteBuddy("io.opentelemetry.android.instrumentation:coroutines-agent")

    coreLibraryDesugaring(libs.desugarJdkLibs)
}

// OkHttp 5 publishes okhttp-jvm, which does not carry the Android variant metadata. Consumers hit
// this too, so the fixture applies the same workaround the demo app does.
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "com.squareup.okhttp3" && requested.name == "okhttp-jvm") {
                useTarget("com.squareup.okhttp3:okhttp:${requested.version}")
                because("choosing okhttp over okhttp-jvm")
            }
        }
    }
}
