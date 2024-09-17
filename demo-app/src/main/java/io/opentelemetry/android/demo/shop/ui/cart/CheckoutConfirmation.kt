package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.opentelemetry.android.demo.shop.ui.products.ProductCard
import java.util.Locale

@Composable
fun CheckoutConfirmationScreen(
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                cartViewModel.clearCart()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shippingInfo = checkoutInfoViewModel.shippingInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Your order is complete!",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "We've sent a confirmation email to ${shippingInfo.email}.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        val cartItems = cartViewModel.cartItems.collectAsState().value
        cartItems.forEach { cartItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductCard(
                    product = cartItem.product,
                    onProductClick = {},
                    modifier = Modifier
                        .width(300.dp)
                        .height(170.dp),
                    isNarrow = true
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Quantity: ${cartItem.quantity}",
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = "Total: \$${String.format(Locale.US, "%.2f", cartItem.totalPrice())}",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(8.dp),
                    )
                }
            }
        }

        Text(
            text = "Total Price: \$${String.format(Locale.US, "%.2f", cartViewModel.getTotalPrice())}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.End
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Shipping Data",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Street: ${shippingInfo.streetAddress}",
                )
                Text(
                    text = "City: ${shippingInfo.city}",
                )
                Text(
                    text = "State: ${shippingInfo.state}",
                )
                Text(
                    text = "Zip Code: ${shippingInfo.zipCode}",
                )
                Text(
                    text = "Country: ${shippingInfo.country}",
                )
            }
        }
    }
}
