package io.opentelemetry.android.demo.ui.shop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.opentelemetry.android.demo.model.Product

@Composable
fun ProductList(products: State<List<Product>>, navController: NavController) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(products.value.size) { index ->
            Row() {
                ProductCard(products.value[index], navController = navController)
            }
        }
        item {
            Box(
                modifier = Modifier.height(50.dp)
            )
        }
    }
}