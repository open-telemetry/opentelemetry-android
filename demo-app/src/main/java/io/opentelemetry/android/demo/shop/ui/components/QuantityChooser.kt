package io.opentelemetry.android.demo.shop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.gothamFont
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color

@Composable
fun QuantityChooser(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
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
                    onQuantityChange(selectedQuantity)
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
            QuantityDropDownItem(i, i == selectedQuantity, onClick = {onQuantitySelected(i)})
        }
    }
}

@Composable
fun QuantityDropDownItem(
    quantity: Int,
    isSelected: Boolean,
    onClick: () -> Unit
){
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = quantity.toString(),
                )
            }
        },
        onClick = onClick
    )

}