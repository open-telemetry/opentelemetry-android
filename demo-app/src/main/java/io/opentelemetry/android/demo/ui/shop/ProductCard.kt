package io.opentelemetry.android.demo.ui.shop

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.opentelemetry.android.demo.TAG
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductCard(product: Product) {
    Card(modifier = Modifier.fillMaxSize()
        .height(200.dp)
        .wrapContentHeight()
        .padding(20.dp),
        onClick = {
            Log.d(TAG, "TODO: Implement me!")
        }
    ) {
        Row(modifier = Modifier.padding(20.dp)) {
            Row {
                Text(product.name, fontFamily = gothamFont)
            }
            Row {
                Text("$ " + product.priceValue())
            }
        }
    }
}