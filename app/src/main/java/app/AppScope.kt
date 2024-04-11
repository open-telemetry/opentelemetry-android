package app

import io.opentelemetry.sdk.trace.data.SpanData
import network.RestApi
import okhttp3.mockwebserver.RecordedRequest

interface AppScope {
    fun dumpData(): List<SpanData>
    fun restApi(): RestApi
    fun recordedRequest(): RecordedRequest?
}