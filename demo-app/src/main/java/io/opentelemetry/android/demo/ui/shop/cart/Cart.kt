package io.opentelemetry.android.demo.ui.shop.cart

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.model.Product


@Composable
fun CartScreen() {
//    val products
    Scaffold(
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
//                    this.items(products) { product -> CartItem(product) }
                }
//                CheckoutButton(totalPrice = products.sumOf { it.price })
                CheckoutButton(totalPrice = 100.0)
            }
        }
    )
}

@Composable
fun CartItem(product: Product) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = product.name, fontSize = 20.sp)
        Text(text = "$${product.priceUsd}", fontSize = 20.sp)
    }
}

@Composable
fun CheckoutButton(totalPrice: Double) {
    Button(
        onClick = { /* TODO: Add checkout logic */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(text = "Checkout ($${String.format("%.2f", totalPrice)})")
    }
}