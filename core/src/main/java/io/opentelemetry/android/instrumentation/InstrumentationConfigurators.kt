package io.opentelemetry.android.instrumentation

import java.util.ServiceLoader

internal class InstrumentationConfigurators
    private constructor(
        private val configurators: Map<String, List<InstrumentationConfigurator<AndroidInstrumentation>>>,
    ) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun create(): InstrumentationConfigurators {
            val configurators = ServiceLoader
                .load(InstrumentationConfigurator::class.java)
                .groupBy { it.instrumentationName }
            return InstrumentationConfigurators(configurators as Map<String, List<InstrumentationConfigurator<AndroidInstrumentation>>>)
        }
    }

    fun configure(instrumentation: AndroidInstrumentation){
        configurators[instrumentation.name]?.forEach { it.configure(instrumentation) }
    }
}
