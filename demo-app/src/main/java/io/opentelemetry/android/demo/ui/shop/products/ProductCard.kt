package io.opentelemetry.android.demo.ui.shop.products

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.opentelemetry.android.demo.clients.ImageLoader
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductCard(product: Product) {
    val imageLoader = ImageLoader(LocalContext.current)
    val sourceProductImage = imageLoader.load(product.picture)
    Bitmap.createScaledBitmap(sourceProductImage, 120, 120, false)

//    Bitmap.createBitmap()
    val cardColors = CardColors(
        containerColor = Color.White, contentColor = Color.Black,
        disabledContentColor = Color.Black, disabledContainerColor = Color.Black
    )
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = cardColors,
        modifier = Modifier.fillMaxSize()
        .height(200.dp)
        .wrapContentHeight()
        .padding(20.dp),
        onClick = {
//            Log.d(TAG, "TODO: Implement me!")
        }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Image(
                        bitmap = sourceProductImage.asImageBitmap(),
                        contentDescription = product.name,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(product.name + "\n\n$" + product.priceValue(), fontFamily = gothamFont,
                        style = TextStyle.Default.copy(textAlign = TextAlign.Right),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 15.dp, top = 25.dp).fillMaxWidth()
                    )

                }
            }
        }
    }
}