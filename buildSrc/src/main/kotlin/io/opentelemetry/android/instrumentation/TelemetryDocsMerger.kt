/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File

internal object TelemetryDocsMerger {
    fun merge(
        moduleName: String,
        scopeNames: List<String>,
        observationFiles: Set<File>,
        resolvedRegistryFile: File,
        localRegistryFile: File,
        outputFile: File,
    ) {
        require(scopeNames.isNotEmpty()) {
            "telemetryDocs.scopeNames must declare at least one instrumentation scope for $moduleName."
        }

        val observations =
            observationFiles
                .sortedBy(File::getAbsolutePath)
                .flatMap(::readObservations)
                .filter { it.scope in scopeNames && it.type in SUPPORTED_SIGNAL_TYPES }
        if (observations.isEmpty()) {
            throw GradleException(
                "No telemetry observations matched $moduleName scopes ${scopeNames.sorted()}. " +
                    "Run its tests with -PcollectTelemetryDocs=true.",
            )
        }

        val registry = readRegistry(resolvedRegistryFile)
        val localGroupIds =
            localRegistryFile
                .readLines()
                .mapNotNull { LOCAL_GROUP_ID.matchEntire(it)?.groupValues?.get(1) }
                .toSet()
        val merged =
            observations
                .groupBy { SignalKey(it.type, it.name, it.scope) }
                .map { (key, signals) ->
                    val registryGroup =
                        registry.groups.firstOrNull {
                            it.type == key.type && it.name == key.name
                        }
                    val attributes =
                        mergeAttributes(
                            moduleName,
                            key,
                            signals,
                            registryGroup,
                            registry.attributeProvenance,
                        )
                    validateCoverage(moduleName, key, attributes, registryGroup, localGroupIds)
                    MergedSignal(
                        key = key,
                        registryId = registryGroup?.id,
                        attributes = attributes,
                    )
                }.sortedWith(compareBy({ it.key.type }, { it.key.name }, { it.key.scope }))

        outputFile.parentFile.mkdirs()
        outputFile.writeText(renderYaml(moduleName, scopeNames.distinct().sorted(), merged))
    }

    private fun readObservations(file: File): List<ObservedSignal> {
        val root = JsonSlurper().parse(file) as? Map<*, *>
            ?: throw GradleException("Telemetry observation file is not a JSON object: $file")
        val signals = root["signals"] as? List<*>
            ?: throw GradleException("Telemetry observation file has no signals array: $file")
        return signals.map { value ->
            val signal = value as? Map<*, *>
                ?: throw GradleException("Invalid signal in telemetry observation file: $file")
            val attributes =
                (signal["attributes"] as? List<*>)
                    .orEmpty()
                    .map { attributeValue ->
                        val attribute = attributeValue as Map<*, *>
                        ObservedAttribute(
                            name = attribute.requiredString("name", file),
                            type = attribute.requiredString("type", file),
                        )
                    }
            ObservedSignal(
                type = signal.requiredString("type", file),
                name = signal.requiredString("name", file),
                scope = signal.requiredString("scope", file),
                attributes = attributes,
            )
        }
    }

    private fun readRegistry(file: File): Registry {
        val root = JsonSlurper().parse(file) as Map<*, *>
        val attributeProvenance = mutableMapOf<String, String>()
        val groups =
            (root["groups"] as List<*>).mapNotNull { value ->
                val group = value as Map<*, *>
                val lineage = group["lineage"] as? Map<*, *>
                val attributeLineage = lineage?.get("attributes") as? Map<*, *>
                val attributes =
                    (group["attributes"] as? List<*>)
                        .orEmpty()
                        .map { attributeValue ->
                            val attribute = attributeValue as Map<*, *>
                            val name = attribute.requiredString("name", file)
                            val provenance = attributeLineage?.get(name) as? Map<*, *>
                            val source =
                                if ((provenance?.get("source_group") as? String)
                                    ?.startsWith("registry.android") == true
                                ) {
                                    "local"
                                } else {
                                    "upstream"
                                }
                            attributeProvenance.merge(name, source) { current, candidate ->
                                if (current == "local" || candidate == "local") "local" else "upstream"
                            }
                            RegistryAttribute(
                                name = name,
                                requirementLevel = attribute["requirement_level"] as? String,
                                provenance = source,
                            )
                        }
                val type = group.requiredString("type", file)
                val name =
                    when (type) {
                        "event", "log", "span" -> group["name"] as? String
                        else -> null
                    }
                if (name == null) {
                    null
                } else {
                    RegistryGroup(
                        id = group.requiredString("id", file),
                        type = type,
                        name = name,
                        attributes = attributes,
                    )
                }
            }
        return Registry(groups, attributeProvenance)
    }

    private fun mergeAttributes(
        moduleName: String,
        key: SignalKey,
        signals: List<ObservedSignal>,
        registryGroup: RegistryGroup?,
        attributeProvenance: Map<String, String>,
    ): List<MergedAttribute> =
        signals
            .flatMap { it.attributes }
            .groupBy(ObservedAttribute::name)
            .map { (name, attributes) ->
                val types = attributes.map(ObservedAttribute::type).distinct()
                if (types.size != 1) {
                    throw GradleException(
                        "$moduleName observed conflicting types for ${key.name} attribute $name: " +
                            types.sorted().joinToString(),
                    )
                }
                MergedAttribute(
                    name = name,
                    type = types.single(),
                    registry =
                        registryGroup
                            ?.attributes
                            ?.firstOrNull { it.name == name }
                            ?.provenance
                            ?: attributeProvenance[name]
                            ?: "none",
                )
            }.sortedBy(MergedAttribute::name)

    private fun validateCoverage(
        moduleName: String,
        key: SignalKey,
        attributes: List<MergedAttribute>,
        registryGroup: RegistryGroup?,
        localGroupIds: Set<String>,
    ) {
        if (registryGroup == null || registryGroup.id !in localGroupIds) {
            return
        }
        val observed = attributes.map(MergedAttribute::name).toSet()
        val missing =
            registryGroup.attributes
                .filter { it.requirementLevel != "opt_in" && it.name !in observed }
                .map(RegistryAttribute::name)
                .sorted()
        if (missing.isNotEmpty()) {
            throw GradleException(
                "$moduleName telemetry coverage is incomplete for ${key.type} ${key.name}. " +
                    "Missing required/recommended attributes: ${missing.joinToString()}.",
            )
        }
    }

    private fun renderYaml(
        moduleName: String,
        scopeNames: List<String>,
        signals: List<MergedSignal>,
    ): String =
        buildString {
            appendLine("# GENERATED FILE - do not edit. Regenerated by :mergeAllTelemetryDocs.")
            appendLine("schema_version: 1")
            appendLine("module: ${moduleName.yamlString()}")
            appendLine("scopes:")
            scopeNames.forEach { appendLine("  - ${it.yamlString()}") }
            appendLine("signals:")
            signals.forEach { signal ->
                appendLine("  - type: ${signal.key.type}")
                appendLine("    name: ${signal.key.name.yamlString()}")
                appendLine("    scope: ${signal.key.scope.yamlString()}")
                appendLine("    registry_id: ${signal.registryId?.yamlString() ?: "null"}")
                if (signal.attributes.isEmpty()) {
                    appendLine("    attributes: []")
                } else {
                    appendLine("    attributes:")
                    signal.attributes.forEach { attribute ->
                        appendLine("      - name: ${attribute.name.yamlString()}")
                        appendLine("        type: ${attribute.type}")
                        appendLine("        registry: ${attribute.registry}")
                    }
                }
            }
        }

    private fun Map<*, *>.requiredString(
        key: String,
        file: File,
    ): String =
        this[key] as? String
            ?: throw GradleException("Missing string '$key' in $file")

    private fun String.yamlString(): String =
        "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

    private val LOCAL_GROUP_ID = Regex("""\s*-\s+id:\s+(\S+)\s*""")
    private val SUPPORTED_SIGNAL_TYPES = setOf("event", "log", "span")
}

private data class SignalKey(
    val type: String,
    val name: String,
    val scope: String,
)

private data class ObservedSignal(
    val type: String,
    val name: String,
    val scope: String,
    val attributes: List<ObservedAttribute>,
)

private data class ObservedAttribute(
    val name: String,
    val type: String,
)

private data class RegistryGroup(
    val id: String,
    val type: String,
    val name: String,
    val attributes: List<RegistryAttribute>,
)

private data class Registry(
    val groups: List<RegistryGroup>,
    val attributeProvenance: Map<String, String>,
)

private data class RegistryAttribute(
    val name: String,
    val requirementLevel: String?,
    val provenance: String,
)

private data class MergedSignal(
    val key: SignalKey,
    val registryId: String?,
    val attributes: List<MergedAttribute>,
)

private data class MergedAttribute(
    val name: String,
    val type: String,
    val registry: String,
)
