package io.opentelemetry.android.demo.ui.shop.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.ui.shop.products.ProductCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.opentelemetry.android.demo.clients.ImageLoader
import io.opentelemetry.android.demo.clients.ProductCatalogClient
import io.opentelemetry.android.demo.clients.RecommendationService
import io.opentelemetry.android.demo.ui.shop.products.RecommendedSection

@Composable
fun CartScreen(cartViewModel: CartViewModel = viewModel(),
               onCheckoutClick: () -> Unit,
               onProductClick: (String) -> Unit
) {
    val context = LocalContext.current
    val productsClient = ProductCatalogClient(context)
    val allProducts = remember { productsClient.get() }
    val cartItems by cartViewModel.cartItems.collectAsState()
    val isCartEmpty = cartItems.isEmpty()
    val recommendationService = RecommendationService()
    val recommendedProducts = remember { recommendationService.getRecommendedProducts(cartItems, allProducts) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            OutlinedButton(
                onClick = { cartViewModel.clearCart() },
                modifier = Modifier
            ) {
                Text("Empty Cart", color = Color.Red)
            }
        }

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
                    text = "Total: \$${String.format(Locale.US, "%.2f", cartItems[index].totalPrice())}",
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

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

