package io.opentelemetry.android.instrumentation

import java.util.ServiceLoader

private typealias Konfigurator = InstrumentationConfigurator<*>

internal class InstrumentationConfigurators
    private constructor(private val configurators: Map<String, List<Konfigurator>>) {

    companion object {
        fun create(): InstrumentationConfigurators {
            return create { ServiceLoader.load(Konfigurator::class.java) }
        }
        // Exists for testing
        inline fun create(loader: () -> Iterable<Konfigurator>): InstrumentationConfigurators {
            val configurators = loader().groupBy { it.instrumentationName }
            return InstrumentationConfigurators(configurators)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun configure(instrumentation: AndroidInstrumentation) {
        configurators[instrumentation.name]
            ?.forEach { (it as InstrumentationConfigurator<AndroidInstrumentation>).configure(instrumentation) }
    }
}