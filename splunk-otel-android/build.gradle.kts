plugins {
    id("com.android.library")
    id("splunk.android-library-conventions")
}

android {
    compileSdk = 32
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        all {
            // provide the version of the library as a resource so it can be used as a span attribute.
            resValue("string", "rum.version", "${project.version}")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

val otelVersion = "1.17.0"
val otelAlphaVersion = "$otelVersion-alpha"

dependencies {
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("androidx.core:core:1.8.0")
    implementation("androidx.navigation:navigation-fragment:2.5.1")

    api(platform("io.opentelemetry:opentelemetry-bom:$otelVersion"))
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")

    implementation(platform("io.opentelemetry:opentelemetry-bom-alpha:$otelAlphaVersion"))
    implementation("io.opentelemetry:opentelemetry-semconv")
    implementation("io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0:$otelAlphaVersion")

    api("io.opentelemetry:opentelemetry-api")
    api("com.squareup.okhttp3:okhttp:4.10.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.7.0")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.robolectric:robolectric:4.8.1")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("org.assertj:assertj-core:3.23.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.6")
}

extra["pomName"] = "Splunk Otel Android"
description = "A library for instrumenting Android applications for Splunk RUM"
