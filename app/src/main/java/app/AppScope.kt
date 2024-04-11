package app

import io.opentelemetry.sdk.trace.data.SpanData
import network.CallableApi
import network.SingleApi
import okhttp3.mockwebserver.RecordedRequest

interface AppScope {
    fun dumpData(): List<SpanData>
    fun singleApi(): SingleApi
    fun callableApi(): CallableApi
    fun recordedRequest(): RecordedRequest?
}