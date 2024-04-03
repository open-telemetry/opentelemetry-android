package com.example.hello_otel

import com.chuckerteam.chucker.api.ChuckerInterceptor
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RestApiUtil {


    fun restApi(app: DemoApp,server: MockWebServer): RestApi {
        val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(FirstFixedInterceptor())
                .addInterceptor(ChuckerInterceptor.Builder(app).createShortcut(true).build())
                .addInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR)
                .addInterceptor(SecondFixedInterceptor())

                .build()
        return Retrofit.Builder()
                .client(client)
                .baseUrl(server.url("rt/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build().create(RestApi::class.java)
    }

}