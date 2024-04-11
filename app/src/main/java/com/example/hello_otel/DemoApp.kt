package com.example.hello_otel

import android.app.Application
import androidx.fragment.app.Fragment
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import io.reactivex.Single
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.http.GET
import retrofit2.http.Header
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
interface RestApi {
    @GET("auth")
    fun login(@Header("x-bypass") flag: Int): Single<UserToken>

    @GET("profile")
    fun profile(@Header("token") flag: String): Single<JsonElement>

}

data class UserToken(@SerializedName("token") val token: String)

interface AppScope {

    fun dumpData(): List<Any>
    fun restApi(): RestApi
    fun recordedRequest(): RecordedRequest?
    fun openTelemetryRum(): OpenTelemetryRum
    fun rootSpan(): Span?
}

class DemoApp : Application(), AppScope {
    private val server = MockWebServer()
    private var rootSpan: Span? = null
    private val inMemorySpanExporter = InMemorySpanExporter.create()
    private val openTelemetryRum: OpenTelemetryRum by lazy {
        val instance = OpenTelemetryRum.builder(this).build()
        GlobalOpenTelemetry.set(instance.openTelemetry)
        instance
    }
    private val restApi by lazy {
        RestApiUtil.restApi(this, server)
    }

    override fun onCreate() {
        super.onCreate()
        plantTimberLogger()
        MockWebServerUtil.initServer(server)
        this.rootSpan = OpenTelemetryUtil.kickOffRootSpan(openTelemetryRum)
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

    override fun openTelemetryRum(): OpenTelemetryRum {
        return openTelemetryRum
    }

    override fun rootSpan(): Span? {
        return rootSpan
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

