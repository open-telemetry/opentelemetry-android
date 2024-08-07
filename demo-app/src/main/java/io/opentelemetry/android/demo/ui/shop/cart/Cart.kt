package io.opentelemetry.android.demo.ui.shop.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.ui.shop.products.ProductCard

@Composable
fun CartScreen(cartViewModel: CartViewModel = viewModel()) {
    val cartItems by cartViewModel.cartItems.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(cartItems.size) { index ->
                ProductCard(product = cartItems[index].product, onClick = {})
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quantity: ${cartItems[index].quantity}",
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total: \$${String.format("%.2f", cartItems[index].totalPrice())}",
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Total Price: \$${String.format("%.2f", cartViewModel.getTotalPrice())}",
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = { /* TODO: Handle checkout */ },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Checkout")
        }
    }
}

