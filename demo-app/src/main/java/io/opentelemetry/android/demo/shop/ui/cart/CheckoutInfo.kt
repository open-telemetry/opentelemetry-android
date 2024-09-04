package io.opentelemetry.android.demo.shop.ui.cart

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.opentelemetry.android.demo.shop.ui.components.UpPressButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

data class ShippingInfo(
    var email: String = "",
    var streetAddress: String = "",
    var zipCode: String = "",
    var city: String = "",
    var state: String = "",
    var country: String = ""
) {
    fun isComplete(): Boolean {
        return arrayOf(email, streetAddress, zipCode, city, state, country)
            .all { it.isNotBlank() }
    }
}

data class PaymentInfo(
    var creditCardNumber: String = "",
    var expiryMonth: String = "",
    var expiryYear: String = "",
    var cvv: String = ""
) {
    fun isComplete(): Boolean {
        return arrayOf(creditCardNumber, expiryMonth, expiryYear, cvv)
            .all { it.isNotBlank() }
    }
}

@Composable
fun InfoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    )
}

@Composable
fun InfoFieldsSection(
    fields: List<Triple<String, String, (String) -> Unit>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        fields.forEach { (label, value, onValueChange) ->
            InfoField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                keyboardType = when (label) {
                    "Zip Code", "Credit Card Number", "Month", "Year", "CVV" -> KeyboardType.Number
                    else -> KeyboardType.Text
                },
                isPassword = label == "CVV"
            )
        }
    }
}

@Composable
fun InfoScreen(
    upPress: () -> Unit
) {
    var shippingInfo by remember { mutableStateOf(ShippingInfo()) }
    var paymentInfo by remember { mutableStateOf(PaymentInfo()) }

    val focusManager = LocalFocusManager.current
    val canProceed = shippingInfo.isComplete() && paymentInfo.isComplete()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Content inside a Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable { focusManager.clearFocus() }
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(title = "Shipping Address")

            InfoFieldsSection(
                fields = listOf(
                    Triple("E-mail Address", shippingInfo.email) { shippingInfo = shippingInfo.copy(email = it) },
                    Triple("Street Address", shippingInfo.streetAddress) { shippingInfo = shippingInfo.copy(streetAddress = it) },
                    Triple("Zip Code", shippingInfo.zipCode) { shippingInfo = shippingInfo.copy(zipCode = it) },
                    Triple("City", shippingInfo.city) { shippingInfo = shippingInfo.copy(city = it) },
                    Triple("State", shippingInfo.state) { shippingInfo = shippingInfo.copy(state = it) },
                    Triple("Country", shippingInfo.country) { shippingInfo = shippingInfo.copy(country = it) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "Payment Method")

            InfoFieldsSection(
                fields = listOf(
                    Triple("Credit Card Number", paymentInfo.creditCardNumber) { paymentInfo = paymentInfo.copy(creditCardNumber = it) },
                    Triple("Month", paymentInfo.expiryMonth) { paymentInfo = paymentInfo.copy(expiryMonth = it) },
                    Triple("Year", paymentInfo.expiryYear) { paymentInfo = paymentInfo.copy(expiryYear = it) },
                    Triple("CVV", paymentInfo.cvv) { paymentInfo = paymentInfo.copy(cvv = it) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /*TODO Handle*/ },
                modifier = Modifier.fillMaxWidth(),
                enabled = canProceed
            ) {
                Text("Proceed")
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
