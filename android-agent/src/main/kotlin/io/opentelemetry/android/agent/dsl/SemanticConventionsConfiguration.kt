package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.common.internal.SemconvCompat

/**
 * Type-safe DSL configuration of semantic conventions behaviors.
 */
@OpenTelemetryDslMarker
class SemanticConventionsConfiguration {

    /**
     * Determines if the latest available experimental semantic conventions should
     * be used. If set to false, the old, deprecated, or nonstandard semantic convention
     * names will continue to be used.
     *
     * This provides a compatibility path forward for users who need to continue using
     * existing values until the next major version bump. This setting does NOT
     * imply that only _stable_ semantic conventions are emitted.
     */
    var useLatestExperimental: Boolean = true
        set(value){
            field = value
            SemconvCompat.useLatestExperimental = value
        }

}
