package io.opentelemetry.android.demo.shop.ui.products

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.opentelemetry.android.demo.shop.model.Product

@Composable
fun ProductList(products: List<Product>, onProductClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(products.size) { index ->
            Row {
                ProductCard(products[index], onProductClick = onProductClick)
            }
        }
        item {
            Box(
                modifier = Modifier.height(50.dp)
            )
        }
    }
}
