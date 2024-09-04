package io.opentelemetry.android.demo.shop.clients

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Fake (for now) image loader. Ideally this would fetch images from the
 * imageloader service.
 */
class ImageLoader(private val context: Context) {

    // `http://localhost:8080/${src}?w=${width}&q=${quality || 75}`
    fun load(name: String): Bitmap {
        val file = "images/${name}"
        val input = context.assets.open(file)
        return BitmapFactory.decodeStream(input)
    }
}