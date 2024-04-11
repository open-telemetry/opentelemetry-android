package app

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import network.MockWebServerUtil
import network.RestApi
import network.RestApiUtil
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import timber.log.Timber
import java.util.concurrent.TimeUnit


class DemoApp : Application(), AppScope {
    private val server = MockWebServer()
    private val inMemorySpanExporter = InMemorySpanExporter.create()
    private val restApi by lazy {
        RestApiUtil.restApi(this, server)
    }

    override fun onCreate() {
        super.onCreate()
        plantTimberLogger()
        MockWebServerUtil.initServer(server)
        OpenTelemetryUtil.configOpenTelemetry(inMemorySpanExporter)
    }


    private fun plantTimberLogger() {
        Timber.plant(Timber.DebugTree())
        Timber.tag(LOG_TAG).i("Demo App started")
    }

    override fun dumpData(): List<SpanData> {
        return inMemorySpanExporter.finishedSpanItems
    }

    override fun restApi(): RestApi {
        return restApi
    }

    override fun recordedRequest(): RecordedRequest? {
        return server.takeRequest(3, TimeUnit.SECONDS)
    }

    companion object {

        const val TRACER = "trace_hello_otel"
        const val LOG_TAG = "trace"
        const val SPAN_COLD_LAUNCH = "span_cold_launch"

        fun appScope(context: android.content.Context): AppScope {
            return context.applicationContext as AppScope
        }
    }
}

