package io.opentelemetry.android.demo.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val picture: String,
    val priceUsd: PriceUsd,
    val categories: List<String>
) {
}

data class PriceUsd(
    val currencyCode: String,
    val units: Long,
    val nanos: Long
)