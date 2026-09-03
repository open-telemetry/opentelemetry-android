import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.opentelemetry.android.COLLECT_TELEMETRY_DOCS_PROPERTY
import io.opentelemetry.android.TELEMETRY_DOCS_DIRECTORY
import io.opentelemetry.android.TELEMETRY_DOCS_OUTPUT_DIRECTORY_PROPERTY
import io.opentelemetry.android.TELEMETRY_OBSERVATION_ELEMENTS_CONFIGURATION
import io.opentelemetry.android.instrumentation.TelemetryDocsExtension

extensions.create<TelemetryDocsExtension>("telemetryDocs")

val collectTelemetryDocs =
    providers.gradleProperty(COLLECT_TELEMETRY_DOCS_PROPERTY).map(String::toBoolean).orElse(false)
val unitTestObservationDirectory =
    layout.buildDirectory.dir(TELEMETRY_DOCS_DIRECTORY).map { it.dir("observations") }
// Android Gradle Plugin pulls PlatformTestStorage output into this directory.
val connectedAndroidTestObservationDirectory =
    layout.buildDirectory.dir("outputs/connected_android_test_additional_output")
val observationDirectories =
    listOf(
        unitTestObservationDirectory,
        connectedAndroidTestObservationDirectory,
    )

tasks.withType<Test>().configureEach {
    val observationDirectory = unitTestObservationDirectory.map { it.dir(name) }
    systemProperty(COLLECT_TELEMETRY_DOCS_PROPERTY, collectTelemetryDocs.get())
    systemProperty(
        TELEMETRY_DOCS_OUTPUT_DIRECTORY_PROPERTY,
        observationDirectory
            .get()
            .asFile
            .absolutePath,
    )
    if (collectTelemetryDocs.get()) {
        outputs.dir(observationDirectory).withPropertyName("telemetryDocsObservations")
        doFirst {
            delete(observationDirectory)
        }
    }
}

plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension> {
        defaultConfig {
            testInstrumentationRunnerArguments[COLLECT_TELEMETRY_DOCS_PROPERTY] =
                collectTelemetryDocs.get().toString()
        }
    }
}

plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension> {
        defaultConfig {
            testInstrumentationRunnerArguments[COLLECT_TELEMETRY_DOCS_PROPERTY] =
                collectTelemetryDocs.get().toString()
        }
    }
}

val telemetryObservationElements =
    configurations.create(TELEMETRY_OBSERVATION_ELEMENTS_CONFIGURATION) {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
observationDirectories.forEach { directory ->
    artifacts.add(telemetryObservationElements.name, directory) {
        type = "directory"
    }
}
