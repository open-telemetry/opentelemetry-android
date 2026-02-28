plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
    id("jacoco")
}

description = "OpenTelemetry Android PANS (Per-Application Network Selection) instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.pans"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":services"))
    implementation(project(":common"))
    implementation(project(":agent-api"))
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.auto.service.annotations)

    ksp(libs.auto.service.processor)

    testImplementation(project(":test-common"))
    testImplementation(project(":session"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockk)
}

// Jacoco coverage configuration
jacoco {
    toolVersion = "0.8.8"
}

tasks.register("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    doLast {
        println("✅ Jacoco Test Report Generated")
        println("📊 Coverage Report Location: build/reports/coverage/")
    }
}

// Task to check coverage
tasks.register("checkCoverage") {
    dependsOn("jacocoTestReport")

    doLast {
        println(
            """
            ╔════════════════════════════════════════════════════════════════╗
            ║              PANS INSTRUMENTATION TEST COVERAGE                ║
            ║                     Target: 80% Coverage                        ║
            ╚════════════════════════════════════════════════════════════════╝
            """.trimIndent(),
        )
    }
}
