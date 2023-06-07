plugins {
    id("com.android.library")
    id("splunk.android-library-conventions")
    id("splunk.errorprone-conventions")
}

android {
    namespace = "opentelemetry.rum.instrumentation"

    compileSdk = 33
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        all {
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

val otelVersion = "1.25.0"
val otelAlphaVersion = "$otelVersion-alpha"
val otelInstrumentationVersion = "1.25.0-SNAPSHOT"
val otelInstrumentationAlphaVersion = "1.25.0-alpha-SNAPSHOT"

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.10.1")
    implementation("androidx.navigation:navigation-fragment:2.5.3")

    api(platform("io.opentelemetry:opentelemetry-bom:$otelVersion"))
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")

    implementation(platform("io.opentelemetry:opentelemetry-bom-alpha:$otelAlphaVersion"))
    implementation("io.opentelemetry:opentelemetry-semconv")

    api("io.opentelemetry:opentelemetry-api")

    testImplementation("org.mockito:mockito-core:5.3.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.0")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.robolectric:robolectric:4.10")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.awaitility:awaitility:4.2.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

extra["pomName"] = "OpenTelemetry Android Instrumentation"
description = "A library for instrumenting Android applications with OpenTelemetry"
