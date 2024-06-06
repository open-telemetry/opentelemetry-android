package io.opentelemetry.android.demo.ui.shop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductList(products: State<List<Product>>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(products.value.size) { index ->
            Row() {
                ProductCard(products.value[index])
            }
        }
        item {
            Box(
                modifier = Modifier.height(50.dp)
            )
        }
    }
}