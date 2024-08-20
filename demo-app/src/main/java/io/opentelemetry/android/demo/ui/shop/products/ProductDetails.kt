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

@Composable
fun ProductDetails(product:Product,
                   cartViewModel: CartViewModel = viewModel(),
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
        }

        UpPressButton(
            upPress = upPress,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
}
