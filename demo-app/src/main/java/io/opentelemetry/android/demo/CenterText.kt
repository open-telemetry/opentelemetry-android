package io.opentelemetry.android.demo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun CenterText(text: String, fontSize: TextUnit = 12.sp) {
    Text(
        text, textAlign = TextAlign.Center,
        fontSize = fontSize,
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle.Default.copy(textAlign = TextAlign.Center)
    )
}