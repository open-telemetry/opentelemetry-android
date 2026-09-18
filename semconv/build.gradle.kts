import io.opentelemetry.android.WEAVER_CONFIGURATION
import io.opentelemetry.android.semconv.DownloadWeaverTask

plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android federated semantic conventions"

android {
    namespace = "io.opentelemetry.android.semconv"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    implementation(libs.opentelemetry.semconv.kotlin)
}

// Matches opentelemetry-kotlin's semconv module: generated code is not Detekt-reviewed.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}

@org.gradle.api.tasks.CacheableTask
abstract class GenerateSemanticConventionsTask
    @Inject
    constructor(
        private val execOps: ExecOperations,
        private val fileOps: FileSystemOperations,
    ) : DefaultTask() {
        @get:InputDirectory
        @get:IgnoreEmptyDirectories
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val modelDir: DirectoryProperty

        @get:InputDirectory
        @get:IgnoreEmptyDirectories
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val templatesDir: DirectoryProperty

        @get:InputFile
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val weaverBinary: RegularFileProperty

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @TaskAction
        fun run() {
            try {
                // Ensure deleted or renamed conventions do not leave stale generated sources behind.
                fileOps.delete {
                    delete(outputDir.get().asFile)
                }
                execOps.exec {
                    commandLine(
                        weaverBinary.get().asFile.absolutePath,
                        "registry",
                        "generate",
                        "-r",
                        modelDir.get().asFile.absolutePath,
                        "--templates",
                        templatesDir.get().asFile.absolutePath,
                        "kotlin",
                        outputDir.get().asFile.absolutePath,
                    )
                }
            } catch (exc: Exception) {
                throw GradleException(
                    "OTel weaver command failed.",
                    exc,
                )
            }
        }
    }

val downloadWeaver =
    tasks.register<DownloadWeaverTask>("downloadWeaver") {
        weaverVersion.set(libs.versions.weaver)
    }

val weaver =
    configurations.create(WEAVER_CONFIGURATION) {
        isCanBeConsumed = true
        isCanBeResolved = false
    }

artifacts.add(
    weaver.name,
    downloadWeaver.flatMap { it.weaverBinary },
) {
    builtBy(downloadWeaver)
}

val generateSemanticConventions =
    tasks.register<GenerateSemanticConventionsTask>("generateSemanticConventions") {
        modelDir.set(layout.projectDirectory.dir("model"))
        templatesDir.set(layout.projectDirectory.dir("templates"))
        outputDir.set(layout.buildDirectory.dir("generated/semconv/kotlin"))
        weaverBinary.set(downloadWeaver.flatMap { it.weaverBinary })
    }

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateSemanticConventions,
            GenerateSemanticConventionsTask::outputDir,
        )
    }
}
