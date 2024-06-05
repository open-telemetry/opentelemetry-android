package io.opentelemetry.android.demo.ui.shop

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductList(products: State<List<Product>>) {
    Text("Welcome to the Astronomy Shop") //todo: delme
    Row {
        products.value.forEach {
            ProductCard(it)
        }
    }
}