/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.semconv

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import javax.inject.Inject

private enum class WeaverOs { MAC, LINUX, WINDOWS }

private enum class WeaverArch(
    val rustName: String,
) {
    X86_64("x86_64"),
    AARCH64("aarch64"),
}

private data class WeaverTarget(
    val os: WeaverOs,
    val triple: String,
    val archiveExtension: String,
) {
    val executableName: String = if (os == WeaverOs.WINDOWS) "weaver.exe" else "weaver"
}

private fun currentWeaverTarget(
    osName: String,
    archName: String,
): WeaverTarget {
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
            // which every musl distro ships at this exact path.
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
@CacheableTask
abstract class DownloadWeaverTask
    @Inject
    constructor(
        private val archiveOps: ArchiveOperations,
        private val execOps: ExecOperations,
        private val fileOps: FileSystemOperations,
        private val objects: ObjectFactory,
        providers: ProviderFactory,
        layout: ProjectLayout,
    ) : DefaultTask() {
        @get:Input
        abstract val weaverVersion: Property<String>

        @get:Input
        abstract val targetTriple: Property<String>

        @get:Input
        abstract val archiveExtension: Property<String>

        @get:OutputFile
        abstract val weaverBinary: RegularFileProperty

        init {
            val weaverTarget =
                providers.systemProperty("os.name").flatMap { osName ->
                    providers.systemProperty("os.arch").map { archName ->
                        currentWeaverTarget(osName.lowercase(), archName.lowercase())
                    }
                }
            targetTriple.convention(weaverTarget.map(WeaverTarget::triple))
            archiveExtension.convention(weaverTarget.map(WeaverTarget::archiveExtension))
            weaverBinary.convention(
                layout.buildDirectory.flatMap { buildDirectory ->
                    weaverVersion.flatMap { version ->
                        weaverTarget.map { target ->
                            buildDirectory.file("weaver/$version/${target.executableName}")
                        }
                    }
                },
            )
        }

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

            val tarExtractDir = File(workDir, "weaver-tar")
            try {
                val entryName = outputFile.name
                val tree =
                    if (isZip) {
                        archiveOps.zipTree(archiveFile)
                    } else {
                        // Gradle cannot read .tar.xz directly, so use the platform tar.
                        extractTar(archiveFile, tarExtractDir)
                        objects.fileTree().from(tarExtractDir)
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
            } finally {
                archiveFile.delete()
                checksumFile.delete()
                tarExtractDir.deleteRecursively()
            }
        }

        private fun download(
            url: String,
            destination: File,
        ) {
            val client =
                HttpClient
                    .newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build()
            val request =
                HttpRequest
                    .newBuilder(URI.create(url))
                    .header("User-Agent", "opentelemetry-android-build")
                    .timeout(Duration.ofSeconds(60))
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

        private fun extractTar(
            archiveFile: File,
            destinationDir: File,
        ) {
            fileOps.delete {
                delete(destinationDir)
            }
            destinationDir.mkdirs()
            try {
                execOps.exec {
                    commandLine("tar", "-xf", archiveFile.absolutePath, "-C", destinationDir.absolutePath)
                }
            } catch (exception: Exception) {
                throw GradleException(
                    "Failed to extract ${archiveFile.name}. Install a tar implementation with .tar.xz support.",
                    exception,
                )
            }
        }
    }
