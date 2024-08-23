package io.opentelemetry.android.demo.clients

import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.ui.shop.cart.CartViewModel

class RecommendationService(
    private val productCatalogClient: ProductCatalogClient,
    private val cartViewModel: CartViewModel
) {

    fun getRecommendedProducts(currentProduct: Product): List<Product> {
        val allProducts = productCatalogClient.get()
        val cartItems = cartViewModel.cartItems.value

        return allProducts.filter { product ->
            product.id != currentProduct.id && cartItems.none { it.product.id == product.id }
        }.shuffled().take(4)
    }

    fun getRecommendedProducts(): List<Product> {
        val allProducts = productCatalogClient.get()
        val cartItems = cartViewModel.cartItems.value

        return allProducts.filter { product ->
            cartItems.none { it.product.id == product.id }
        }.shuffled().take(4)
    }
}

