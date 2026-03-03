/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.internal

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor
import io.opentelemetry.instrumentation.library.okhttp.OkHttpInstrumentation
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.ConnectionErrorSpanInterceptor
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpClientInstrumenterBuilderFactory
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.TracingInterceptor
import okhttp3.Interceptor
import java.util.function.UnaryOperator

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
object OkHttpSingletons {
    private val NOOP_INTERCEPTOR =
        Interceptor { chain: Interceptor.Chain ->
            chain.proceed(chain.request())
        }

    @JvmField
    var connectionErrorInterceptor: Interceptor = NOOP_INTERCEPTOR

    @JvmField
    var tracingInterceptor: Interceptor = NOOP_INTERCEPTOR

    fun configure(
        instrumentation: OkHttpInstrumentation,
        openTelemetry: OpenTelemetry,
    ) {
        var instrumenterBuilder =
            OkHttpClientInstrumenterBuilderFactory
                .create(openTelemetry)
                .setCapturedRequestHeaders(instrumentation.capturedRequestHeaders)
                .setCapturedResponseHeaders(instrumentation.capturedResponseHeaders)
                .setKnownMethods(instrumentation.knownMethods)
                // Note: The instrumentation allows configuring/overriding the known
                // methods, so even if the underlying extractor has them, we have
                // to pass them along here.
                .setSpanNameExtractorCustomizer(
                    UnaryOperator {
                        HttpSpanNameExtractor
                            .builder(
                                OkHttpAttributesGetter.INSTANCE,
                            ).setKnownMethods(instrumentation.knownMethods)
                            .build()
                    },
                ).addAttributesExtractor(
                    PeerServiceAttributesExtractor.create(
                        OkHttpAttributesGetter.INSTANCE,
                        instrumentation.newPeerServiceResolver(),
                    ),
                ).setEmitExperimentalHttpClientTelemetry(
                    instrumentation.emitExperimentalHttpClientTelemetry(),
                )

        for (extractor in instrumentation.additionalExtractors) {
            instrumenterBuilder = instrumenterBuilder.addAttributesExtractor(extractor)
        }

        val instrumenter = instrumenterBuilder.build()

        connectionErrorInterceptor = ConnectionErrorSpanInterceptor(instrumenter)
        tracingInterceptor = TracingInterceptor(instrumenter, openTelemetry.propagators)
    }

    @JvmField
    val CALLBACK_CONTEXT_INTERCEPTOR: Interceptor =
        Interceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            val context = OkHttpCallbackAdviceHelper.tryRecoverPropagatedContextFromCallback(request)
            context?.makeCurrent()?.use {
                return@Interceptor chain.proceed(request)
            }
            chain.proceed(request)
        }

    @JvmField
    val RESEND_COUNT_CONTEXT_INTERCEPTOR: Interceptor =
        Interceptor { chain: Interceptor.Chain ->
            HttpClientRequestResendCount.initialize(Context.current()).makeCurrent().use {
                chain.proceed(chain.request())
            }
        }
}
