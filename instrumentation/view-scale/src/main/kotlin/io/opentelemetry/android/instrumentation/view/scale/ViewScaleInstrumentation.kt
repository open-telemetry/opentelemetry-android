package io.opentelemetry.android.instrumentation.view.scale


import android.app.Application
import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.internal.InternalViewApi

@AutoService(AndroidInstrumentation::class)
class ViewScaleInstrumentation : AndroidInstrumentation {
    override val name: String = "view.scale"

    @InternalViewApi
    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        (context as? Application)?.registerActivityLifecycleCallbacks(
            ViewScaleActivityCallback(
                ViewScaleEventGenerator(
                    openTelemetryRum.openTelemetry
                        .logsBridge
                        .loggerBuilder("io.opentelemetry.android.instrumentation.view.click")
                        .build(),
                    context
                ),
            ),
        )
    }
}
