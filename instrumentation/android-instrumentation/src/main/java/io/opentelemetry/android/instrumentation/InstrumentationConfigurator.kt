package io.opentelemetry.android.instrumentation

/**
 * Configures a discovered [AndroidInstrumentation] before it is installed.
 *
 * @param T The instrumentation type this configurator can configure.
 */
interface InstrumentationConfigurator<in T : AndroidInstrumentation> {

    /**
     * The [AndroidInstrumentation.name] value of the instrumentation this configurator applies to.
     * This name must exactly match the name returned by the AndroidInstrumentation.name.
     */
    val instrumentationName: String

    /**
     * Called to configure an [instrumentation] before [AndroidInstrumentation.install] is called.
     */
    fun configure(instrumentation: T)
}
