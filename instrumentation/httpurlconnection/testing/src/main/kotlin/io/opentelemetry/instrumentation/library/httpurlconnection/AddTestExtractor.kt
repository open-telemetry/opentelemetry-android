/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.InstrumentationConfigurator
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.net.URLConnection

@AutoService(InstrumentationConfigurator::class)
class AddTestExtractor : InstrumentationConfigurator<HttpUrlInstrumentation> {
    override val instrumentationType = HttpUrlInstrumentation::class.java

    override fun configure(instrumentation: HttpUrlInstrumentation) {
        val extractor: AttributesExtractor<URLConnection, Int?> =
            object : AttributesExtractor<URLConnection, Int?> {
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
