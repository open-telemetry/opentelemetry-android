import java.time.Duration

plugins {
    id("com.android.library")
    id("splunk.android-library-conventions")
}

android {
    compileSdk = 31
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        all {
            //provide the version of the library as a resource so it can be used as a span attribute.
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

        unitTests.all {
            it.testLogging.showStandardStreams = true
            it.testLogging {
                events("started", "passed", "failed")
            }
        }
    }
}


dependencies {
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.navigation:navigation-fragment:2.4.1")
    compileOnly("com.android.volley:volley:1.2.1")

    implementation(project(":splunk-otel-android"))

    api(platform("io.opentelemetry:opentelemetry-bom:1.11.0"))
    api("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")

    implementation(platform("io.opentelemetry:opentelemetry-bom-alpha:1.11.0-alpha"))
    implementation("io.opentelemetry:opentelemetry-semconv")

    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:1.11.0-alpha"))
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.robolectric:robolectric:4.7.3")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("com.google.mockwebserver:mockwebserver:20130706")
    testImplementation("com.android.volley:volley:1.2.0")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.13")

}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(15))
}

extra["pomName"] = "Splunk Otel Android Volley"
description = "A library for instrumenting Android applications using Volley Library for Splunk RUM"
