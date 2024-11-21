package io.opentelemetry.android.demo.shop.model

import androidx.annotation.Keep

@Keep
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val picture: String,
    val priceUsd: PriceUsd,
    val categories: List<String>
) {
    fun priceValue(): Double {
        return priceUsd.units.toDouble() + priceUsd.nanos.toDouble() / 1_000_000_000f
    }
}

// For deserialization
@Keep
data class ProductDeserializationWrapper(
    val products: List<Product>
)

@Keep
data class PriceUsd(
    val currencyCode: String,
    val units: Long,
    val nanos: Long
)