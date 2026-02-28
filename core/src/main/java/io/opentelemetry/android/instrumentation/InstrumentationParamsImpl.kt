package io.opentelemetry.android.instrumentation

import android.app.Application
import android.content.Context
import io.opentelemetry.android.instrumentation.InstrumentationParams
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.Clock

internal class InstrumentationParamsImpl(
    override val context: Context,
    override val openTelemetry: OpenTelemetry,
    override val sessionProvider: SessionProvider,
    override val clock: Clock,
): InstrumentationParams {
    override val application: Application? = context as? Application
}