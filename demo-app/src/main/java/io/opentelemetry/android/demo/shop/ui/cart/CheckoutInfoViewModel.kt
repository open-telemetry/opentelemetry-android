package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class ShippingInfo(
    var email: String = "someone@example.com",
    var streetAddress: String = "1600 Amphitheatre Parkway",
    var zipCode: String = "94043",
    var city: String = "Mountain View",
    var state: String = "CA",
    var country: String = "United States"
) {
    fun isComplete(): Boolean {
        return arrayOf(email, streetAddress, zipCode, city, state, country)
            .all { it.isNotBlank() }
    }
}

data class PaymentInfo(
    var creditCardNumber: String = "4432-8015-6152-0454",
    var expiryMonth: String = "01",
    var expiryYear: String = "2030",
    var cvv: String = "137"
) {
    fun isComplete(): Boolean {
        return arrayOf(creditCardNumber, expiryMonth, expiryYear, cvv)
            .all { it.isNotBlank() }
    }
}

class CheckoutInfoViewModel : ViewModel() {

    var shippingInfo by mutableStateOf(ShippingInfo())
        private set

    var paymentInfo by mutableStateOf(PaymentInfo())
        private set

    fun updateShippingInfo(newShippingInfo: ShippingInfo) {
        shippingInfo = newShippingInfo
    }

    fun updatePaymentInfo(newPaymentInfo: PaymentInfo) {
        paymentInfo = newPaymentInfo
    }

    fun canProceedToCheckout(): Boolean {
        return shippingInfo.isComplete() && paymentInfo.isComplete()
    }
}