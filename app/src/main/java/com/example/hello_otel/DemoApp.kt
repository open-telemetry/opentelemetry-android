package com.example.hello_otel

import android.app.Application
import androidx.fragment.app.Fragment
import com.google.gson.annotations.SerializedName
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
interface RestApi {

    @GET("auth/{userName}")
    fun login(@Path("userName") userName: String): Single<UserToken>
}

data class UserToken(@SerializedName("token") val token: String)

interface AppScope {

    fun restApi(): RestApi
}

class DemoApp : Application(), AppScope {

    private val server = MockWebServer()
    private val restApi by lazy {
        val client: OkHttpClient = OkHttpClient.Builder()
                //Pay attention that this is done without any context related to open telemetry.
                .addNetworkInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR)
                .build()
        Retrofit.Builder()
                .client(client)
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build().create(RestApi::class.java)

    }

    override fun onCreate() {
        super.onCreate()
        initServer()
        plantTimberLogger()
        initOpenTelemetry()
    }

    private fun initServer() {
        Completable.fromAction {
            server.start()
            Timber.i(" $restApi is initialized.")
            repeat(10) {
                server.enqueue(MockResponse().setResponseCode(200).setBody("""
                {
                    "token":"1234"
                }
            """.trimIndent()))
            }
        }.subscribeOn(Schedulers.computation()).subscribe()

    }

    private fun initOpenTelemetry() {


        //step 1: config the telemetrySdk
        val inMemorySpanExporter = InMemorySpanExporter.create()
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val spanProcessor: SpanProcessor = SimpleSpanProcessor.create(inMemorySpanExporter)
        val sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .build()
        GlobalOpenTelemetry.set(telemetrySdk)


        val tracer: Tracer = GlobalOpenTelemetry.getTracer("AppLaunchTracer")
        val spanBuilder: SpanBuilder = tracer.spanBuilder("A Test Span")
        val baggage = Baggage.builder()
                .put("user.name", "tonytang")
                .build()
        val makeCurrent: Scope = Context.current().with(baggage).makeCurrent()
        makeCurrent.use {
            spanBuilder.setAttribute("root_key_1", "root_key_2")
            spanBuilder.setSpanKind(SpanKind.CLIENT)
            val rootSpan = spanBuilder.startSpan()
            rootSpan.addEvent("started_event")

            //act
            rootSpan.makeCurrent()
            rootSpan.addEvent("ended_event")

        }

    }

    private fun plantTimberLogger() {
        Timber.plant(Timber.DebugTree())
        Timber.i("Demo App started")
    }

    override fun restApi(): RestApi {
        return restApi
    }

    companion object {

        fun appScope(context: android.content.Context): AppScope {
            return context.applicationContext as AppScope
        }
    }
}