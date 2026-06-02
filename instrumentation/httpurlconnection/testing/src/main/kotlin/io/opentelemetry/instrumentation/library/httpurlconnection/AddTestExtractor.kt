package io.opentelemetry.instrumentation.library.httpurlconnection

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.InstrumentationConfigurator
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.net.URLConnection

@AutoService(InstrumentationConfigurator::class)
class AddExtractor : InstrumentationConfigurator<HttpUrlInstrumentation> {
    override val instrumentationName: String = "httpurlconnection"

    override fun configure(instrumentation: HttpUrlInstrumentation) {
        val extractor = object : AttributesExtractor<URLConnection, Int> {
            override fun onStart(
                builder: AttributesBuilder,
                ctx: Context,
                urlConnection: URLConnection,
            ) {
                builder.put("extractor.on.start", true)
            }

            override fun onEnd(
                builder: AttributesBuilder,
                ctx: Context,
                urlConnection: URLConnection,
                response: Int?,
                err: Throwable?,
            ) {
                builder.put("extractor.on.end", true)
            }
        }
        instrumentation.addAttributesExtractor(extractor)
    }
}
