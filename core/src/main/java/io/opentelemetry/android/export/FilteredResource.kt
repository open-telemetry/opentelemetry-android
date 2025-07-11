package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.resources.ResourceBuilder

internal class FilteredResource(
    private val resource: Resource,
    private val omitKeys: List<String>
) {

    fun get(): Resource {
        val builder = Resource.builder().setSchemaUrl(resource.schemaUrl)
        resource.attributes.forEach { key, value ->
            if (wantKey(key)) {
                put(builder, key, value)
            }
        }
        return builder.build();
    }

    private fun put(
        builder: ResourceBuilder,
        key: AttributeKey<*>,
        value: Any
    ) {
        when (key.type) {
            AttributeType.STRING -> builder.put(
                key as AttributeKey<String>,
                value as String
            )

            AttributeType.LONG -> builder.put(key as AttributeKey<Long>, value as Long)
            AttributeType.DOUBLE -> builder.put(
                key as AttributeKey<Double>,
                value as Double
            )

            AttributeType.BOOLEAN -> builder.put(
                key as AttributeKey<Boolean>,
                value as Boolean
            )

            AttributeType.STRING_ARRAY -> builder.put(
                key as AttributeKey<List<String>>,
                value as List<String>
            )

            AttributeType.LONG_ARRAY -> builder.put(
                key as AttributeKey<List<Long>>,
                value as List<Long>
            )

            AttributeType.DOUBLE_ARRAY -> builder.put(
                key as AttributeKey<List<Double>>,
                value as List<Double>
            )

            AttributeType.BOOLEAN_ARRAY -> builder.put(
                key as AttributeKey<List<Boolean>>,
                value as List<Boolean>
            )
        }
    }

    private fun wantKey(key: AttributeKey<*>): Boolean {
        return !omitKeys.contains(key.key)
    }
}