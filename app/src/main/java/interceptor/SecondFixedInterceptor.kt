package interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import java.io.IOException

internal class SecondFixedInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(request(chain))
    }

    private fun request(chain: Interceptor.Chain): Request {
        val requestBuilder: Builder = chain.request().newBuilder()
        requestBuilder.addHeader("fixed_header_key_2", "fixed_header_value_2")
        return requestBuilder.build()
    }
}