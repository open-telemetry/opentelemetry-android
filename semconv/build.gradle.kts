import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Only used to strip the outer XZ layer off weaver's release archives before handing
        // the resulting plain tar to Gradle's own tarTree()/FileSystemOperations for extraction.
        classpath(libs.tukaani.xz)
    }
}

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

// Matches opentelemetry-kotlin's semconv module: generated code is not Detekt-reviewed.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}

enum class WeaverOs { MAC, LINUX, WINDOWS }

enum class WeaverArch(
    val rustName: String,
) {
    X86_64("x86_64"),
    AARCH64("aarch64"),
}

data class WeaverTarget(
    val os: WeaverOs,
    val triple: String,
    val archiveExtension: String,
)

fun currentWeaverTarget(): WeaverTarget {
    val osName = System.getProperty("os.name").lowercase()
    val archName = System.getProperty("os.arch").lowercase()
    // Only match architectures weaver actually publishes.
    val arch =
        when (archName) {
            "aarch64", "arm64" -> WeaverArch.AARCH64
            "x86_64", "amd64" -> WeaverArch.X86_64
            else -> throw GradleException("Unsupported architecture for downloading weaver: $archName")
        }
    return when {
        osName.contains("mac") || osName.contains("darwin") -> {
            WeaverTarget(WeaverOs.MAC, "${arch.rustName}-apple-darwin", "tar.xz")
        }

        osName.contains("win") -> {
            // weaver publishes no aarch64-windows build, so x86_64 is used unconditionally;
            // it still runs on ARM64 Windows via its built-in x64 emulation.
            WeaverTarget(WeaverOs.WINDOWS, "x86_64-pc-windows-msvc", "zip")
        }

        osName.contains("nux") || osName.contains("nix") -> {
            // musl-based distros (e.g. Alpine, common in slim CI containers) need the "-musl"
            // build instead of the default glibc one. Detect via musl's own dynamic linker,
            // which every musl distro ships at this exact path, rather than a specific distro.
            val libc = if (File("/lib/ld-musl-${arch.rustName}.so.1").exists()) "musl" else "gnu"
            WeaverTarget(WeaverOs.LINUX, "${arch.rustName}-unknown-linux-$libc", "tar.xz")
        }

        else -> {
            throw GradleException("Unsupported OS for downloading weaver: $osName")
        }
    }
}

/**
 * Downloads the `weaver` CLI (https://github.com/open-telemetry/weaver) release binary matching
 * the current OS/arch, verifies its checksum, and extracts it. Cached per-version under `build/`,
 * so contributors and CI never need to install weaver themselves.
 */
abstract class DownloadWeaverTask
    @Inject
    constructor(
        private val archiveOps: ArchiveOperations,
        private val fileOps: FileSystemOperations,
    ) : DefaultTask() {
        @get:Input
        abstract val weaverVersion: Property<String>

        @get:Input
        abstract val targetTriple: Property<String>

        @get:Input
        abstract val archiveExtension: Property<String>

        @get:OutputFile
        abstract val weaverBinary: RegularFileProperty

        @TaskAction
        fun run() {
            val version = weaverVersion.get()
            val triple = targetTriple.get()
            val extension = archiveExtension.get()
            val isZip = extension == "zip"
            val assetName = "weaver-$triple.$extension"
            val releaseUrl =
                "https://github.com/open-telemetry/weaver/releases/download/v$version/$assetName"

            val outputFile = weaverBinary.get().asFile
            val workDir = outputFile.parentFile
            workDir.mkdirs()

            val archiveFile = File(workDir, assetName)
            val checksumFile = File(workDir, "$assetName.sha256")
            download(releaseUrl, archiveFile)
            download("$releaseUrl.sha256", checksumFile)
            verifyChecksum(archiveFile, checksumFile)

            val entryName = outputFile.name
            val tarFile = File(workDir, "weaver.tar")
            val tree =
                if (isZip) {
                    archiveOps.zipTree(archiveFile)
                } else {
                    decompressXz(archiveFile, tarFile)
                    archiveOps.tarTree(tarFile)
                }

            // Release archives nest the binary under a top-level "weaver-<triple>/" directory;
            // flatten it since we only want the single executable, at a known path.
            fileOps.copy {
                from(tree.matching { include("**/$entryName") })
                into(workDir)
                eachFile { relativePath = RelativePath(true, entryName) }
                includeEmptyDirs = false
            }
            check(outputFile.isFile) {
                "Could not find '$entryName' inside $assetName"
            }

            if (!isZip) {
                outputFile.setExecutable(true)
            }

            archiveFile.delete()
            checksumFile.delete()
            tarFile.delete()
        }

        private fun download(
            url: String,
            destination: File,
        ) {
            val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
            val request =
                HttpRequest
                    .newBuilder(URI.create(url))
                    .header("User-Agent", "opentelemetry-android-build")
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofFile(destination.toPath()))
            check(response.statusCode() == 200) {
                "Failed to download $url: HTTP ${response.statusCode()}"
            }
        }

        private fun verifyChecksum(
            archiveFile: File,
            checksumFile: File,
        ) {
            val expected =
                checksumFile
                    .readText()
                    .trim()
                    .split(Regex("\\s+"))
                    .first()
            val digest = MessageDigest.getInstance("SHA-256")
            archiveFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual == expected) {
                "Checksum mismatch for ${archiveFile.name}: expected $expected but got $actual"
            }
        }

        private fun decompressXz(
            source: File,
            destination: File,
        ) {
            org.tukaani.xz.XZInputStream(source.inputStream().buffered()).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

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

val weaverTarget = currentWeaverTarget()
val weaverExecutableName = if (weaverTarget.os == WeaverOs.WINDOWS) "weaver.exe" else "weaver"

val downloadWeaver =
    tasks.register<DownloadWeaverTask>("downloadWeaver") {
        weaverVersion.set(libs.versions.weaver)
        targetTriple.set(weaverTarget.triple)
        archiveExtension.set(weaverTarget.archiveExtension)
        weaverBinary.set(
            layout.buildDirectory.file("weaver/${libs.versions.weaver.get()}/$weaverExecutableName"),
        )
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
