package io.opentelemetry.android.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import io.opentelemetry.api.trace.SpanKind

private const val SMILEY = "\uD83D\uDE0A"
private const val NEUTRAL = "\uD83D\uDE10"
private const val FROWN = "☹\uFE0F"

@Preview
@Composable
fun Disposition() {
    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton(onClick = { dispositionClickEvent("happy", SMILEY) }) {
            Text(text = SMILEY, fontSize = 75.sp)
        }
        TextButton(onClick = { dispositionClickEvent("neutral", NEUTRAL) }) {
            Text(text = NEUTRAL, fontSize = 75.sp)
        }
        TextButton(onClick = { dispositionClickEvent("sad", FROWN) }) {
            Text(text = FROWN, fontSize = 75.sp)
        }
    }
}

fun dispositionClickEvent(disposition: String, emoji: String) {
    val tracer = BackpackingBuddyApplication.tracer("bb.disposition")
    val span = tracer
        ?.spanBuilder("bb.disposition")
        ?.setSpanKind(SpanKind.INTERNAL)
        ?.setAttribute("status", disposition)
        ?.setAttribute("emoji", emoji)
        ?.startSpan()
    span?.end()
}