package io.opentelemetry.android.instrumentation

import android.app.Application
import android.content.Context
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.Clock

interface InstrumentationParams {
    val context: Context
    val openTelemetry: OpenTelemetry
    val sessionProvider: SessionProvider
    val clock: Clock
    val application: Application?
}
