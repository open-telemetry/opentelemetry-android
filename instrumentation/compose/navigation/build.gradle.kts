plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

// The Compose compiler Gradle plugin is on the buildSrc classpath, so it is applied by id (without
// pulling a second copy from the plugin portal).
apply(plugin = "org.jetbrains.kotlin.plugin.compose")

// Under AGP's built-in Kotlin the Compose plugin can't detect the Kotlin version, so it requests
// the compiler-embeddable artifact with an empty version. Pin it to the catalog Kotlin version.
configurations.matching { it.name.startsWith("kotlinCompilerPluginClasspath") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" &&
            requested.name == "kotlin-compose-compiler-plugin-embeddable" &&
            requested.version.isNullOrEmpty()
        ) {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

description = "OpenTelemetry Android compose navigation instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.compose.navigation"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests {
            // Required so Robolectric reads the merged manifest (which declares the
            // ComponentActivity that Compose's createComposeRule launches).
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing

    implementation(project(":agent-api"))
    implementation(project(":common"))
    implementation(project(":semconv"))

    compileOnly(libs.compose) {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "androidx.savedstate")
    }
    compileOnly(libs.androidx.navigation.compose)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.semconv.kotlin)

    testImplementation(project(":test-common"))

    testImplementation(libs.compose)
    testImplementation(libs.androidx.navigation.compose)
    testImplementation(libs.compose.ui.test.junit4)
    // Provides the ComponentActivity that createComposeRule launches; merged into the manifest the
    // Robolectric unit tests run against.
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
