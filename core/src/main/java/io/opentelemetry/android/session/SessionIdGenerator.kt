package io.opentelemetry.android.session

import io.opentelemetry.api.trace.TraceId
import java.util.Random

interface SessionIdGenerator {

    fun generateSessionId(): String

    object DEFAULT: SessionIdGenerator {
        override fun generateSessionId(): String {
            val random = Random()
            // The OTel TraceId has exactly the same format as a RUM SessionId, so let's re-use it here,
            // rather than re-inventing the wheel.
            return TraceId.fromLongs(random.nextLong(), random.nextLong())
        }
    }
}