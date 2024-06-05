package io.opentelemetry.android.demo.ui.shop

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductList(products: State<List<Product>>) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(products.value.size) { index ->
            Row() {
                ProductCard(products.value[index])
            }
        }
    }
}