import java.time.Duration

plugins {
    id("otel.android-library-conventions")
}

android {
    namespace = "io.opentelemetry.android.volley"

    buildToolsVersion = "34.0.0"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true

        unitTests.all {
            it.testLogging.showStandardStreams = true
            it.testLogging {
                events("started", "passed", "failed")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.semconv)
    compileOnly(libs.volley)
    coreLibraryDesugaring(libs.desugarJdkLibs)

    api(platform(libs.opentelemetry.platform))
    api(libs.opentelemetry.api)

    testImplementation(libs.volley)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(15))
}

description = "An OpenTelemetry Android library for instrumenting Volley HTTP clients"
