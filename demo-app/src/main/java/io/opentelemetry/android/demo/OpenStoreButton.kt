package io.opentelemetry.android.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.api.trace.SpanKind

@Composable
fun OpenStoreButton(
    text: String,
    onClick: () -> Unit
) {
    Row {
        Spacer(modifier = Modifier.height(5.dp))
    }
    Row {
        Button(
            onClick = onClick,
            border = BorderStroke(1.dp, Color.Gray),
            modifier = Modifier.padding(20.dp).height(90.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
                contentColor = Color(0xFF425CC7)),
            content = {
                CenterText(text = text, fontSize = 30.sp)
            }
        )
    }
}
