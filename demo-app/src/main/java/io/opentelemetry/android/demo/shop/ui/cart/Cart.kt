package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductCatalogClient
import io.opentelemetry.android.demo.shop.clients.RecommendationService
import io.opentelemetry.android.demo.shop.ui.products.ProductCard
import io.opentelemetry.android.demo.shop.ui.products.RecommendedSection
import io.opentelemetry.api.common.AttributeKey.doubleKey
import java.util.Locale

@Composable
fun CartScreen(
    cartViewModel: CartViewModel = viewModel(),
    onCheckoutClick: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val context = LocalContext.current
    val productsClient = ProductCatalogClient(context)
    val recommendationService = remember { RecommendationService(productsClient, cartViewModel) }
    val cartItems by cartViewModel.cartItems.collectAsState()
    val isCartEmpty = cartItems.isEmpty()
    val recommendedProducts = remember { recommendationService.getRecommendedProducts() }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                OutlinedButton(
                    onClick = { clearCart(cartViewModel) },
                    modifier = Modifier
                ) {
                    Text("Empty Cart", color = Color.Red)
                }
            }
        }

        items(cartItems.size) { index ->
            ProductCard(product = cartItems[index].product, onProductClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quantity: ${cartItems[index].quantity}",
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: \$${String.format(Locale.US, "%.2f", cartItems[index].totalPrice())}",
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total Price: \$${String.format(Locale.US, "%.2f", cartViewModel.getTotalPrice())}",
                modifier = Modifier.padding(16.dp)
            )

            Button(
                onClick = onCheckoutClick,
                enabled = !isCartEmpty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCartEmpty) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Checkout")
            }

            Spacer(modifier = Modifier.height(32.dp))
            RecommendedSection(recommendedProducts = recommendedProducts, onProductClick = onProductClick)
        }
    }
}

private fun clearCart(cartViewModel: CartViewModel) {
    generateEmptiedCartEvent(cartViewModel)
    cartViewModel.clearCart()
}

private fun generateEmptiedCartEvent(cartViewModel: CartViewModel) {
    val eventBuilder = OtelDemoApplication.eventBuilder("otel.demo.app", "cart.emptied")
    eventBuilder.setAttribute(doubleKey("cart.total.value"), cartViewModel.getTotalPrice())
        .emit()
}
