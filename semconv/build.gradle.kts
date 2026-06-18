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
}

// Matches opentelemetry-kotlin's semconv module: generated code is committed and not Detekt-reviewed.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}

abstract class GenerateSemanticConventionsTask
    @Inject
    constructor(
        private val execOps: ExecOperations,
    ) : DefaultTask() {
        @get:InputDirectory
        abstract val modelDir: DirectoryProperty

        @get:InputDirectory
        abstract val templatesDir: DirectoryProperty

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @TaskAction
        fun run() {
            try {
                execOps.exec {
                    commandLine(
                        "weaver",
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
                    "OTel weaver command failed. Install weaver and make sure it is on PATH.",
                    exc,
                )
            }
        }
    }

tasks.register<GenerateSemanticConventionsTask>("generateSemanticConventions") {
    modelDir.set(layout.projectDirectory.dir("model"))
    templatesDir.set(layout.projectDirectory.dir("templates"))
    outputDir.set(layout.projectDirectory.dir("src/main/kotlin"))
}
