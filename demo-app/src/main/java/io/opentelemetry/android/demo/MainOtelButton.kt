package io.opentelemetry.android.demo

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

@Composable
fun MainOtelButton(
    icon: Painter,
    text: String,
    onClick: () -> Unit
) {
    Row {
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.padding(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            content = {
                Image(
                    painter = icon,
                    contentDescription = null,
                    Modifier
                        .width(150.dp)
                        .padding(30.dp)
                )
            })
    }
    Row {
        Text(text, fontSize = 30.sp)
    }
}