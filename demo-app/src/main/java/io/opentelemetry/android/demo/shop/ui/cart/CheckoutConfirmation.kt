package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.shop.ui.products.ProductCard

@Composable
fun CheckoutConfirmationScreen(
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel
) {
    val shippingInfo = checkoutInfoViewModel.shippingInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Your order is complete!",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "We've sent you a confirmation email.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        val cartItems = cartViewModel.cartItems.collectAsState().value
        cartItems.forEach { cartItem ->
            ProductCard(
                product = cartItem.product,
                onProductClick = {},
                isNarrow = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Text(
                text = "Quantity: ${cartItem.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = "Total: $${String.format("%.2f", cartItem.totalPrice())}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.End
            )
        }

        Text(
            text = "Grand Total: $${cartViewModel.getTotalPrice()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.End
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.LightGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Shipping Data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Text(
                    text = "Street: ${shippingInfo.streetAddress}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    text = "City: ${shippingInfo.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    text = "State: ${shippingInfo.state}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    text = "Zip Code: ${shippingInfo.zipCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    text = "Country: ${shippingInfo.country}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }

        Button(
            onClick = { /* TODO Handle continue shopping action */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Continue Shopping")
        }
    }
}
