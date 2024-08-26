package io.opentelemetry.android.demo.clients

import io.opentelemetry.android.demo.model.Product
import io.opentelemetry.android.demo.ui.shop.cart.CartViewModel

class RecommendationService(
    private val productCatalogClient: ProductCatalogClient,
    private val cartViewModel: CartViewModel
) {

    private fun getAllNonCartProducts(): List<Product>{
        val allProducts = productCatalogClient.get()
        val cartItems = cartViewModel.cartItems.value

        return allProducts.filter { product -> cartItems.none { it.product.id == product.id } }
    }

    fun getRecommendedProducts(currentProduct: Product, numberOfProducts: Int = 4): List<Product> {
        return getAllNonCartProducts().filter { it.id != currentProduct.id }
        .shuffled().take(numberOfProducts)
    }

    fun getRecommendedProducts(numberOfProducts: Int = 4): List<Product> {
        return getAllNonCartProducts().shuffled().take(numberOfProducts)
    }
}

