package io.opentelemetry.android.agent

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader

class FakeInstrumentationLoader : AndroidInstrumentationLoader {
    override fun <T : AndroidInstrumentation> getByType(
        type: Class<out T>
    ): T? = null

    override fun getAll(): Collection<AndroidInstrumentation> = emptyList()
}
