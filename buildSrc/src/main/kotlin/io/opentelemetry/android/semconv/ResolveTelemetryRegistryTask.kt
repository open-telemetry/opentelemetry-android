/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.semconv

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
abstract class ResolveTelemetryRegistryTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {
        @get:InputFile
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val weaverBinary: RegularFileProperty

        @get:InputDirectory
        @get:IgnoreEmptyDirectories
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val registryDirectory: DirectoryProperty

        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        @TaskAction
        fun resolve() {
            val output = outputFile.get().asFile
            output.parentFile.mkdirs()

            try {
                execOperations.exec {
                    commandLine(
                        weaverBinary.get().asFile.absolutePath,
                        "registry",
                        "resolve",
                        "-r",
                        registryDirectory.get().asFile.absolutePath,
                        "--skip-policies",
                        "--include-unreferenced",
                        "--format",
                        "json",
                        "--output",
                        output.absolutePath,
                        "--quiet",
                    )
                }
            } catch (exception: Exception) {
                throw GradleException("Failed to resolve the telemetry semantic convention registry.", exception)
            }
        }
    }
