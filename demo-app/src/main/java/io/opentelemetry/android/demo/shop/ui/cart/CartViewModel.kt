package io.opentelemetry.android.demo.shop.ui.cart

import androidx.lifecycle.ViewModel
import io.opentelemetry.android.demo.shop.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CartItem(
    val product: Product,
    var quantity: Int
) {
    fun totalPrice() = product.priceValue() * quantity
}

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    fun addProduct(product: Product, quantity: Int) {
        _cartItems.value = _cartItems.value.toMutableList().apply {
            val index = indexOfFirst { it.product.id == product.id }
            if (index >= 0) {
                this[index] = this[index].copy(quantity = this[index].quantity + quantity)
            } else {
                add(CartItem(product, quantity))
            }
        }
    }

    fun getTotalPrice(): Double {
        return _cartItems.value.sumOf { it.totalPrice() }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

}
