package io.opentelemetry.android.demo.clients

import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.ui.shop.cart.CartItem

class RecommendationService {

    fun getRecommendedProducts(currentProduct: Product, cartItems: List<CartItem>, allProducts: List<Product>): List<Product> {
        val filteredProducts = allProducts.filter { product ->
            product.id != currentProduct.id && cartItems.none { it.product.id == product.id }
        }
        return filteredProducts.shuffled().take(4)
    }

    fun getRecommendedProducts(cartItems: List<CartItem>, allProducts: List<Product>): List<Product> {
        val filteredProducts = allProducts.filter { product ->
           cartItems.none { it.product.id == product.id }
        }
        return filteredProducts.shuffled().take(4)
    }
}
