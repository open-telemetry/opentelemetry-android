package io.opentelemetry.android.demo.ui.shop.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class ShippingInfo(
    var email: String = "",
    var streetAddress: String = "",
    var zipCode: String = "",
    var city: String = "",
    var state: String = "",
    var country: String = ""
)

data class PaymentInfo(
    var creditCardNumber: String = "",
    var expiryMonth: String = "",
    var expiryYear: String = "",
    var cvv: String = ""
)

@Composable
fun InfoScreen() {
    var shippingInfo by remember { mutableStateOf(ShippingInfo()) }
    var paymentInfo by remember { mutableStateOf(PaymentInfo()) }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
            .clickable { focusManager.clearFocus() }
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Shipping Address",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = shippingInfo.email,
            onValueChange = { shippingInfo = shippingInfo.copy(email = it) },
            label = { Text("E-mail Address") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shippingInfo.streetAddress,
            onValueChange = { shippingInfo = shippingInfo.copy(streetAddress = it) },
            label = { Text("Street Address") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shippingInfo.zipCode,
            onValueChange = { shippingInfo = shippingInfo.copy(zipCode = it) },
            label = { Text("Zip Code") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = shippingInfo.city,
            onValueChange = { shippingInfo = shippingInfo.copy(city = it) },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shippingInfo.state,
            onValueChange = { shippingInfo = shippingInfo.copy(state = it) },
            label = { Text("State") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shippingInfo.country,
            onValueChange = { shippingInfo = shippingInfo.copy(country = it) },
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = paymentInfo.creditCardNumber,
            onValueChange = { paymentInfo = paymentInfo.copy(creditCardNumber = it) },
            label = { Text("Credit Card Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = paymentInfo.expiryMonth,
                onValueChange = { paymentInfo = paymentInfo.copy(expiryMonth = it) },
                label = { Text("Month") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = paymentInfo.expiryYear,
                onValueChange = { paymentInfo = paymentInfo.copy(expiryYear = it) },
                label = { Text("Year") },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        OutlinedTextField(
            value = paymentInfo.cvv,
            onValueChange = { paymentInfo = paymentInfo.copy(cvv = it) },
            label = { Text("CVV") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /*TODO Handle*/ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Proceed")
        }
    }
}