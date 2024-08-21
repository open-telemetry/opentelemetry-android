package io.opentelemetry.android.demo.ui.shop.products

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
import io.opentelemetry.android.demo.clients.ImageLoader
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.ui.shop.components.QuantityChooser
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.ui.shop.cart.CartViewModel
import io.opentelemetry.android.demo.ui.shop.components.UpPressButton
import androidx.compose.ui.Alignment
import io.opentelemetry.android.demo.model.PriceUsd

@Composable
fun ProductDetails(product:Product,
                   cartViewModel: CartViewModel = viewModel(),
                   onProductClick: (String) -> Unit,
                   upPress: () -> Unit
){
    val imageLoader = ImageLoader(LocalContext.current)
    val sourceProductImage = imageLoader.load(product.picture)
    var quantity by remember { mutableIntStateOf(1) }

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
            Button(
                onClick = { cartViewModel.addProduct(product, quantity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "Add to Cart")
            }
            val recommendedProducts = listOf(
                Product(
                    id = "66VCHSJNUP",
                    name = "Starsense Explorer Refractor Telescope",
                    description = "The first telescope that uses your smartphone to analyze the night sky and calculate its position in real time.",
                    picture = "StarsenseExplorer.jpg",
                    priceUsd = PriceUsd(currencyCode = "USD", units = 349, nanos = 950000000),
                    categories = listOf("telescopes")
                ),
                Product(
                    id = "0PUK6V6EV0",
                    name = "Solar System Color Imager",
                    description = "The NexImage 10 Solar System Imager is the perfect solution for capturing impressive deep-sky astroimages.",
                    picture = "SolarSystemColorImager.jpg",
                    priceUsd = PriceUsd(currencyCode = "USD", units = 175, nanos = 0),
                    categories = listOf("accessories", "telescopes")
                ),
                Product(
                    id = "LS4PSXUNUM",
                    name = "Red Flashlight",
                    description = "A 3-in-1 device featuring a red flashlight, hand warmer, and portable power bank, perfect for nighttime activities.",
                    picture = "RedFlashlight.jpg",
                    priceUsd = PriceUsd(currencyCode = "USD", units = 57, nanos = 80000000),
                    categories = listOf("accessories", "flashlights")
                ),
                Product(
                    id = "2ZYFJ3GM2N",
                    name = "Roof Binoculars",
                    description = "This versatile binocular is a great choice for nature observation and bird watching.",
                    picture = "RoofBinoculars.jpg",
                    priceUsd = PriceUsd(currencyCode = "USD", units = 209, nanos = 950000000),
                    categories = listOf("binoculars")
                ),
                Product(
                    id = "L9ECAV7KIM",
                    name = "Lens Cleaning Kit",
                    description = "A cleaning kit for all glass and optical surfaces, perfect for maintaining clear optics.",
                    picture = "LensCleaningKit.jpg",
                    priceUsd = PriceUsd(currencyCode = "USD", units = 21, nanos = 950000000),
                    categories = listOf("accessories")
                )
            )
            Spacer(modifier = Modifier.height(32.dp))
            RecommendedSection(recommendedProducts = recommendedProducts, onProductClick = onProductClick)
        }

        UpPressButton(
            upPress = upPress,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
}

