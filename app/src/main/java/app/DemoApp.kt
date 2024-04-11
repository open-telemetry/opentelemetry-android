package app

import android.app.Application
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import network.CallableApi
import network.MockWebServerUtil
import network.RestApiUtil
import network.SingleApi
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import timber.log.Timber
import java.util.concurrent.TimeUnit


class DemoApp : Application(), AppScope {
    private val server = MockWebServer()
    private val inMemorySpanExporter = InMemorySpanExporter.create()

    private val retrofit by lazy {
        RestApiUtil.retrofit(this, server)
    }

    private val singleApi by lazy {
        retrofit.create(SingleApi::class.java)
    }

    private val callableApi by lazy {
        retrofit.create(CallableApi::class.java)
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

    override fun singleApi(): SingleApi {
        return singleApi
    }

    override fun callableApi(): CallableApi {
        return callableApi
    }

    override fun recordedRequest(): RecordedRequest? {
        return server.takeRequest(2, TimeUnit.SECONDS)
    }

    companion object {
        const val LOG_TAG = "trace"
        fun appScope(context: AppContext): AppScope {
            return context.context.applicationContext as AppScope
        }
    }
}

