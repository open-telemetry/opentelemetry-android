package io.opentelemetry.android.instrumentation.common

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context

/**
 * Extractor of point-in-time attributes. Whereas the AttributesExtractor in the
 * upstream instrumentation api is targeted at tracing/spans (onStart, onEnd),
 * this extractor operates purely on the parentContext and a generic SUBJECT
 * at a point in time. The resulting Attributes will all be added to the event
 * that is being generated.
 */
interface EventAttributesExtractor<SUBJECT> {

    fun extract(parentContext: Context, subject: SUBJECT): Attributes
}