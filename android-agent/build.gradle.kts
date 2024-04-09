plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

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
    implementation(project(":common"))
    implementation(project(":instrumentation:activity"))
    implementation(project(":instrumentation:anr"))
    implementation(project(":instrumentation:common-api"))
    implementation(project(":instrumentation:crash"))
    implementation(project(":instrumentation:network"))
    implementation(project(":instrumentation:slowrendering"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.navigation.fragment)

    api(platform(libs.opentelemetry.platform))
    api(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.logging)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.diskBuffering)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.awaitility)
}

extra["pomName"] = "OpenTelemetry Android Instrumentation"
description = "A library for instrumenting Android applications with OpenTelemetry"
