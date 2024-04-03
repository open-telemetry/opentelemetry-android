package com.example.hello_otel

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageEntry
import io.opentelemetry.context.Context
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import java.io.IOException

internal class FirstFixedInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val requestBuilder: Builder = originalRequest.newBuilder()
        // Retrieve current context and baggage
        val currentContext = Context.current()
        val currentBaggage = Baggage.fromContext(currentContext)
        // Inject baggage into request headers
        currentBaggage.forEach { key: String, value: BaggageEntry -> requestBuilder.addHeader(key, value.value) }
        requestBuilder.addHeader("fixed_header_key_1", "fixed_header_value_1")
        val requestWithBaggage: Request = requestBuilder.build()
        return chain.proceed(requestWithBaggage)
    }
}