package io.opentelemetry.instrumentation.library.okhttp.v3_0

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class FixedTestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Chain): Response {
        val requestWithFixedHeader = withFixedHeader(chain)
        Thread.sleep(100)
        return chain.proceed(requestWithFixedHeader)
    }

    companion object {
        private fun withFixedHeader(chain: Chain): Request {
            return chain.request().newBuilder().addHeader("fixed_header_key", "fixed_header_value").build()
        }
    }
}