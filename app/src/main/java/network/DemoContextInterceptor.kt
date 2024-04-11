package network

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class DemoContextInterceptor : Interceptor {
    private val httpHeadersSetter = TextMapSetter<Request.Builder> { carrier, key, value -> carrier?.header(key, value) }
    private val context: Context = Context.current().with(attachedBaggage())

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val newBuilder: Request.Builder = chain.request().newBuilder()
        GlobalOpenTelemetry.get().propagators.textMapPropagator.inject(context, newBuilder, httpHeadersSetter)
        return chain.proceed(newBuilder.build())
    }

    private fun attachedBaggage(): Baggage {
        return Baggage.builder()
                .put("cold_launch_id_2", "fixed_cold_launch_id1")
                .build()
    }

}