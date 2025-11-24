plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

android {
    namespace = "io.opentelemetry.android"

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

    sourceSets {
        getByName("test") {
            kotlin.srcDirs("src/integrationTest/kotlin")
        }
    }
}

dependencies {
    implementation(project(":agent-api"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":common"))
    implementation(project(":services"))
    implementation(project(":session"))

    implementation(libs.androidx.core)

    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.opentelemetry.exporter.logging)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.diskBuffering)
    implementation(libs.opentelemetry.processors)
    testImplementation(libs.opentelemetry.api.incubator)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit.ktx)
}

extra["pomName"] = "OpenTelemetry Android Instrumentation"
description = "A library for instrumenting Android applications with OpenTelemetry"
