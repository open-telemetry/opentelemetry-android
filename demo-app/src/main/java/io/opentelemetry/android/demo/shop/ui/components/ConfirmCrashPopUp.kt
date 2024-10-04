package io.opentelemetry.android.demo.shop.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmPopup(
    text : String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = "Are you sure?")
        },
        text = {
            Text(text = text)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(text = "Yes, I'm sure")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = "No, go back")
            }
        }
    )
}
