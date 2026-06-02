package io.opentelemetry.android.instrumentation

import java.util.ServiceLoader

private typealias Konfigurator = InstrumentationConfigurator<AndroidInstrumentation>

internal class InstrumentationConfigurators
    private constructor(private val configurators: Map<String, List<Konfigurator>>) {

    companion object {
        fun create(): InstrumentationConfigurators {
            return create { s -> ServiceLoader.load(s) }
        }

        // Exists for testing
        @Suppress("UNCHECKED_CAST")
        fun create(loader: Loader<Konfigurator>): InstrumentationConfigurators {
            val configurators = loader.load(Konfigurator::class.java)
                .groupBy { it.instrumentationName }
            return InstrumentationConfigurators(configurators)
        }
    }

    fun configure(instrumentation: AndroidInstrumentation){
        configurators[instrumentation.name]?.forEach { it.configure(instrumentation) }
    }
}

/**
 * Exists for testing
 */
internal fun interface Loader<S: Konfigurator> {
    fun load(service: Class<S>): Iterable<S>
}
