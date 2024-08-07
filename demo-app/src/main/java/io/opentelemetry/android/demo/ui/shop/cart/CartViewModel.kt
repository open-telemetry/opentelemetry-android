package io.opentelemetry.android.demo.ui.shop.cart
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


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
}