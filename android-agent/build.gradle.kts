
plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

// This submodule is alpha and is not yet intended to be used by itself
version = project.version.toString().replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")

android {
    namespace = "io.opentelemetry.android"

    buildToolsVersion = "34.0.0"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "OTEL_ANDROID_VERSION", "\"$version\"")
    }

    buildTypes {
        all {
            resValue("string", "rum.version", "${project.version}")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    androidComponents {
        onVariants {
            if (it.buildType == "release") { // The one we choose to release
                project.tasks.register("createReleaseBuild", Copy::class) {
                    from(it.artifacts.get(com.android.build.api.artifact.SingleArtifact.AAR))
                    into(project.layout.buildDirectory.dir("outputs/aar"))
                    rename(".+", "opentelemetry-android.aar")
                }
            }
        }
    }

    project.afterEvaluate {
        tasks.named("assembleRelease") {
            finalizedBy("createReleaseBuild")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":common"))
    api(project(":instrumentation:activity"))
    api(project(":instrumentation:anr"))
    api(project(":instrumentation:common-api"))
    api(project(":instrumentation:crash"))
    api(project(":instrumentation:network"))
    api(project(":instrumentation:slowrendering"))
    api(libs.androidx.appcompat)
    api(libs.androidx.core)
    api(libs.androidx.navigation.fragment)

    api(platform(libs.opentelemetry.platform))
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.exporter.logging)
    api(libs.opentelemetry.instrumentation.api)
    api(libs.opentelemetry.semconv)
    api(libs.opentelemetry.diskBuffering)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.awaitility)
}

extra["pomName"] = "OpenTelemetry Android Instrumentation"
description = "A library for instrumenting Android applications with OpenTelemetry"
