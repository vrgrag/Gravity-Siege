package com.gravitysiege.gravitysiegegame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object AssetBitmaps {
    private val cache = LruCache<String, Bitmap>(64)
    private val images = HashMap<String, ImageBitmap>()

    fun peek(path: String): Bitmap? = cache.get(path)?.takeUnless { it.isRecycled }

    fun imageBitmap(path: String): ImageBitmap? {
        images[path]?.let { return it }
        val bmp = peek(path) ?: return null
        return bmp.asImageBitmap().also { images[path] = it }
    }

    @Synchronized
    fun get(context: Context, path: String): Bitmap? {
        peek(path)?.let { return it }
        val decoded = decode(context, path) ?: return null
        cache.put(path, decoded)
        images[path] = decoded.asImageBitmap()
        return decoded
    }

    private fun decode(context: Context, path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val maxSide = 768
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
    }
}
