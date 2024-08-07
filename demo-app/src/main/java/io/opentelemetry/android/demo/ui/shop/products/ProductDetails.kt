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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment

@Composable
fun ProductDetails(product:Product){
    val imageLoader = ImageLoader(LocalContext.current)
    val sourceProductImage = imageLoader.load(product.picture)

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
        QuantityChooser()
        Spacer(modifier = Modifier.height(16.dp))
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

@Composable
fun QuantityChooser() {
    var quantity by remember { mutableIntStateOf(1) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quantity",
            fontFamily = gothamFont,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Box(
            modifier = Modifier.weight(1f)
        ) {
            QuantityButton(quantity = quantity, onClick = { expanded = true })
            QuantityDropdown(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                selectedQuantity = quantity,
                onQuantitySelected = { selectedQuantity ->
                    quantity = selectedQuantity
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun QuantityButton(quantity: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(48.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = quantity.toString(),
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Dropdown Icon",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun QuantityDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedQuantity: Int,
    onQuantitySelected: (Int) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        for (i in 1..10) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (i == selectedQuantity) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = i.toString(),
                        )
                    }
                },
                onClick = { onQuantitySelected(i) }
            )
        }
    }
}
