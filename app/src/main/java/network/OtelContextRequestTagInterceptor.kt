package network

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class OtelContextRequestTagInterceptor : Interceptor {

    private val httpHeadersSetter = TextMapSetter<Request.Builder> { carrier, key, value -> carrier?.header(key, value) }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rawRequest = chain.request()
        val context = rawRequest.tag(Context::class.java)
        return if (context != null) {
            chain.proceed(injectContext(context, rawRequest))
        } else {
            chain.proceed(rawRequest)
        }
    }

    private fun injectContext(context: Context, rawRequest: Request): Request {
        val newBuilder: Request.Builder = rawRequest.newBuilder()
        GlobalOpenTelemetry.get().propagators.textMapPropagator.inject(context, newBuilder, httpHeadersSetter)
        return newBuilder.build()
    }


}