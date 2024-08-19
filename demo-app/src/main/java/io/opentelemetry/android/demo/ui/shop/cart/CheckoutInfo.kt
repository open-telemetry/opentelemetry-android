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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

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
        modifier = Modifier.padding(vertical = 8.dp)
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
fun InfoScreen() {
    var shippingInfo by remember { mutableStateOf(ShippingInfo()) }
    var paymentInfo by remember { mutableStateOf(PaymentInfo()) }

    val focusManager = LocalFocusManager.current


    val canProceed = shippingInfo.allFieldsNotBlank() && paymentInfo.allFieldsNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
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
}

fun <T : Any> T.allFieldsNotBlank(): Boolean {
    return this::class.memberProperties.all { property ->
        property.isAccessible = true
        val value = property.call(this)
        value is String && value.isNotBlank()
    }
}


