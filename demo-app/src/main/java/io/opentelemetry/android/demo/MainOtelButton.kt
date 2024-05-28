/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.opentelemetry.api.trace.SpanKind

@Composable
fun MainOtelButton(icon: Painter) {
    Row {
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick = { generateClickEvent() },
            modifier = Modifier.padding(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            content = {
                Image(
                    painter = icon,
                    contentDescription = null,
                    Modifier
                        .width(150.dp)
                        .padding(30.dp),
                )
            },
        )
    }
}

fun generateClickEvent() {
    // TODO: Make an actual otel event
    val tracer = OtelSampleApplication.tracer("otel.demo")
    val span =
        tracer
            ?.spanBuilder("logo.clicked")
            ?.setSpanKind(SpanKind.INTERNAL)
            ?.startSpan()
    span?.end()
}
