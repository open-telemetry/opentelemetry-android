package io.opentelemetry.instrumentation.library.okhttp.v3_0

import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RestApiUtil {


    fun restApi(server: MockWebServer): RestApi {
        val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR)
                .addInterceptor(FixedTestInterceptor())
                .build()
        return Retrofit.Builder()
                .client(client)
                .baseUrl(server.url("rt/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build().create(RestApi::class.java)
    }

}