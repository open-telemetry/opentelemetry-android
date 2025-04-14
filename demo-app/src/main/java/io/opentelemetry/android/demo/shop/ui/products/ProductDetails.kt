package io.opentelemetry.android.demo.shop.ui.products

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.shop.clients.ImageLoader
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.ui.components.QuantityChooser
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.components.UpPressButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import io.opentelemetry.android.demo.shop.clients.ProductCatalogClient
import io.opentelemetry.android.demo.shop.clients.RecommendationService
import io.opentelemetry.android.demo.shop.ui.components.SlowCometAnimation
import io.opentelemetry.android.demo.shop.ui.components.ConfirmPopup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Composable
fun ProductDetails(
    product: Product,
    cartViewModel: CartViewModel = viewModel(),
    onProductClick: (String) -> Unit,
    upPress: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader(context)
    val sourceProductImage = imageLoader.load(product.picture)
    var quantity by remember { mutableIntStateOf(1) }

    var slowRender by remember { mutableStateOf(false) }

    val productsClient = ProductCatalogClient(context)
    val recommendationService = remember { RecommendationService(productsClient, cartViewModel) }
    val recommendedProducts = remember { recommendationService.getRecommendedProducts(product) }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                bitmap = sourceProductImage.asImageBitmap(),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product.name,
                fontFamily = gothamFont,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product.description,
                color = Color.Gray,
                textAlign = TextAlign.Justify,
                fontFamily = gothamFont,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$${product.priceValue()}",
                fontFamily = gothamFont,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            QuantityChooser(quantity = quantity, onQuantityChange = { quantity = it })
            Spacer(modifier = Modifier.height(16.dp))
            AddToCartButton(
                cartViewModel = cartViewModel,
                product = product,
                quantity = quantity,
                onSlowRenderChange = { slowRender = it })
            Spacer(modifier = Modifier.height(32.dp))
            RecommendedSection(recommendedProducts = recommendedProducts, onProductClick = onProductClick)
        }

        UpPressButton(
            upPress = upPress,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
        if (slowRender) {
            SlowCometAnimation(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )
        }
    }
}

@Composable
fun AddToCartButton(
    cartViewModel: CartViewModel,
    product: Product,
    quantity: Int,
    onSlowRenderChange: (Boolean) -> Unit
) {
    var showCrashPopup by remember { mutableStateOf(false) }
    var showANRPopup by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (product.id == "OLJCESPC7Z") {
                if (quantity == 10) showCrashPopup = true
                if (quantity == 9) showANRPopup = true
            } else {
                if (product.id == "HQTGWGPNH4") {
                    onSlowRenderChange(true)
                }
            }
            cartViewModel.addProduct(product, quantity)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text = "Add to Cart")
    }

    if (showCrashPopup) {
        ConfirmPopup(
            text = "This will crash the app",
            onConfirm = {
                multiThreadCrashing()
            },
            onDismiss = {
                showCrashPopup = false
            }
        )
    }
    if (showANRPopup) {
        ConfirmPopup(
            text = "This will freeze the app",
            onConfirm = {
                appFreezing()
            },
            onDismiss = {
                showCrashPopup = false
            }
        )
    }
}

fun multiThreadCrashing(numThreads : Int = 4) {
    val latch = CountDownLatch(1)

    for (i in 0..numThreads) {
        val thread = Thread {
            try {
                if (latch.await(10, TimeUnit.SECONDS)) {
                    throw IllegalStateException("Failure from thread ${Thread.currentThread().name}")
                }
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
        thread.name = "crash-thread-$i"
        thread.start()
    }

    try {
        Thread.sleep(100)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        return
    }
    latch.countDown()
}

fun appFreezing(){
    try {
        for (i in 0 .. 20) {
            Thread.sleep(1_000)
        }
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }

}
