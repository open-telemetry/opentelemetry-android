package io.opentelemetry.android.instrumentation

import android.content.Context
import io.opentelemetry.android.OpenTelemetryRum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InstrumentationConfiguratorsTest {

    @Test
    fun testCreateAndConfigure(){
        val config1 = object: InstrumentationConfigurator<TestInstrumentation1> {
            override val instrumentationName: String = "inst1"

            override fun configure(instrumentation: TestInstrumentation1) {
                instrumentation.configured = true
            }

        }
        val config2 = object: InstrumentationConfigurator<TestInstrumentation2> {
            override val instrumentationName: String = "inst2"

            override fun configure(instrumentation: TestInstrumentation2) {
                instrumentation.configured = true
            }

        }
        val inst1 = TestInstrumentation1()
        val inst2 = TestInstrumentation2()
        val inst3 = TestInstrumentation1()
        inst3.name = "not going to find this but that's ok"

        val instrumentations = listOf<InstrumentationConfigurator<*>>(config1, config2)

        val loader: (Class<InstrumentationConfigurator<*>>) -> Iterable<InstrumentationConfigurator<*>> =
            { _ -> instrumentations }

        val configurators = InstrumentationConfigurators.create(loader)
        configurators.configure(inst1)
        configurators.configure(inst2)
        configurators.configure(inst3)

        assertThat(inst1.configured).isTrue
        assertThat(inst2.configured).isTrue

    }
}

class TestInstrumentation1: AndroidInstrumentation {
    override var name: String = "inst1"
    var configured = false

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum
    ) {
    }
}

class TestInstrumentation2: AndroidInstrumentation {
    override var name: String = "inst2"
    var configured = false

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum
    ) {
    }
}
