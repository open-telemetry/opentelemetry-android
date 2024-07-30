package io.opentelemetry.android.demo.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.clients.ImageLoader
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.model.PriceUsd
import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.theme.DemoAppTheme

@Composable
fun ProductDetails(product:Product){
    val imageLoader = ImageLoader(LocalContext.current)
    val sourceProductImage = imageLoader.load(product.picture)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
        Button(
            onClick = { /* TODO: Handle add to cart */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(text = "Add to Cart")
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun ProductDetailsPreview() {
    val price = PriceUsd("USD", 349, 950000000)
    val categories = listOf("telescopes")
    val product = Product("66VCHSJNUP",
        "Starsense Explorer Refractor Telescope",
        "The first telescope that uses your smartphone to analyze the night sky and calculate its position in real time. StarSense Explorer is ideal for beginners thanks to the app’s user-friendly interface and detailed tutorials. It’s like having your own personal tour guide of the night sky",
     "StarsenseExplorer.jpg",
        price,
        categories)
    DemoAppTheme {
        ProductDetails(product)
    }
}