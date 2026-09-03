import io.opentelemetry.android.TELEMETRY_DOCS_DIRECTORY
import io.opentelemetry.android.TELEMETRY_OBSERVATION_ELEMENTS_CONFIGURATION
import io.opentelemetry.android.WEAVER_CONFIGURATION
import io.opentelemetry.android.instrumentation.MergeTelemetryDocsTask
import io.opentelemetry.android.instrumentation.TelemetryDocsExtension
import io.opentelemetry.android.semconv.ResolveTelemetryRegistryTask

val weaverExecutable = configurations.create("weaverExecutable") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies {
    weaverExecutable(project(":semconv", WEAVER_CONFIGURATION))
}

val semconvModelDirectory = layout.projectDirectory.dir("semconv/model")
val documentationTaskGroup = "documentation"
val resolveTelemetryRegistry =
    tasks.register<ResolveTelemetryRegistryTask>("resolveTelemetryRegistry") {
        group = documentationTaskGroup
        description = "Resolves the local and upstream semantic convention registry."
        registryDirectory.set(semconvModelDirectory)
        outputFile.set(layout.buildDirectory.file("$TELEMETRY_DOCS_DIRECTORY/resolved-registry.json"))
        weaverBinary.set(
            layout.file(weaverExecutable.elements.map { it.single().asFile }),
        )
    }

val mergeAllTelemetryDocs =
    tasks.register("mergeAllTelemetryDocs") {
        group = documentationTaskGroup
        description = "Merges captured telemetry for every participating instrumentation module."
    }

val rootBuild = project
val instrumentationDirectory = layout.projectDirectory.dir("instrumentation").asFile.canonicalFile
val aggregations = mutableMapOf<String, InstrumentationAggregation>()

data class InstrumentationAggregation(
    val observationConfiguration: Configuration,
    val mergeTask: TaskProvider<MergeTelemetryDocsTask>,
)

fun getInstrumentationRootDir(projectDir: File): File {
    var currentDirectory = projectDir
    while (currentDirectory.startsWith(instrumentationDirectory)) {
        if (File(currentDirectory, "README.md").isFile) {
            return currentDirectory
        }
        currentDirectory = currentDirectory.parentFile
    }
    throw GradleException(
        "Project $projectDir applies otel.android.telemetry-docs but is not " +
            "inside an instrumentation directory containing README.md.",
    )
}

fun createAggregation(instrumentationName: String): InstrumentationAggregation {
    val taskSuffix =
        instrumentationName
            .split(Regex("[^A-Za-z0-9]+"))
            .filter(String::isNotEmpty)
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
    val observationConfiguration =
        rootBuild.configurations.create(
            "${taskSuffix.replaceFirstChar(Char::lowercase)}TelemetryObservations",
        ) {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
    val mergeTask =
        rootBuild.tasks.register<MergeTelemetryDocsTask>("merge${taskSuffix}TelemetryDocs") {
            group = documentationTaskGroup
            description = "Merges captured telemetry for the $instrumentationName instrumentation."
            moduleName.set(instrumentationName)
            observationFiles.from(
                observationConfiguration.asFileTree.matching {
                    include("**/*.telemetry.json")
                },
            )
            resolvedRegistryFile.set(resolveTelemetryRegistry.flatMap { it.outputFile })
            localRegistryFile.set(semconvModelDirectory.file("android/events.yaml"))
            outputFile.set(File(instrumentationDirectory, "$instrumentationName/telemetry.yaml"))
        }
    mergeAllTelemetryDocs.configure { dependsOn(mergeTask) }
    return InstrumentationAggregation(observationConfiguration, mergeTask)
}

fun registerCaptureProject(captureProject: Project) {
    val instrumentationRoot = getInstrumentationRootDir(captureProject.projectDir.canonicalFile)
    val instrumentationName =
        instrumentationRoot
            .relativeTo(instrumentationDirectory)
            .invariantSeparatorsPath
    val telemetryDocs = captureProject.extensions.getByType<TelemetryDocsExtension>()
    val aggregation = aggregations.getOrPut(instrumentationName) { createAggregation(instrumentationName) }

    rootBuild.dependencies {
        add(
            aggregation.observationConfiguration.name,
            project(captureProject.path, TELEMETRY_OBSERVATION_ELEMENTS_CONFIGURATION),
        )
    }
    aggregation.mergeTask.configure {
        scopeNames.addAll(telemetryDocs.scopeNames)
    }
}

subprojects {
    pluginManager.withPlugin("otel.android.telemetry-docs") {
        registerCaptureProject(this@subprojects)
    }
}
