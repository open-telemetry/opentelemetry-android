package io.opentelemetry.android.demo.shop.clients

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.ProductDeserializationWrapper

const val PRODUCTS_FILE = "products.json"

/** A fake (for now!) client for the ProductCatalog */
// TODO: breedx-splk add instrumentation!
// TODO: Allow this to be wired up to the go service from the actual demo
class ProductCatalogClient(private val context: Context) {

    fun get(): List<Product> {
        val input = context.assets.open(PRODUCTS_FILE)
        val jsonStr = input.bufferedReader()
        val wrapper = Gson().fromJson(jsonStr, ProductDeserializationWrapper::class.java)
        return wrapper.products
    }
}