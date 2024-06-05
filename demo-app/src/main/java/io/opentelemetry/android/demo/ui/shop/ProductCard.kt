package io.opentelemetry.android.demo.ui.shop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductCard(product: Product) {
    Card(modifier = Modifier.size(width = 295.dp, height = 75.dp)) {
        Text(product.name)
    }
}