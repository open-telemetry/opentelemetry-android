package io.opentelemetry.android.instrumentation

import java.util.ServiceLoader

private typealias Konfigurator = InstrumentationConfigurator<*>

internal class InstrumentationConfigurators
    private constructor(private val configurators: Map<String, List<Konfigurator>>) {

    companion object {
        fun create(): InstrumentationConfigurators {
            return create { s -> ServiceLoader.load(s) }
        }

        // Exists for testing
        fun create(loader: Loader): InstrumentationConfigurators {
            val configurators = loader.load(Konfigurator::class.java)
                .groupBy { it.instrumentationName }
            return InstrumentationConfigurators(configurators)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun configure(instrumentation: AndroidInstrumentation) {
        configurators[instrumentation.name]
            ?.forEach { (it as InstrumentationConfigurator<AndroidInstrumentation>).configure(instrumentation) }
    }
}

/**
 * Exists for testing
 */
internal fun interface Loader {
    fun load(service: Class<Konfigurator>): Iterable<Konfigurator>
}
