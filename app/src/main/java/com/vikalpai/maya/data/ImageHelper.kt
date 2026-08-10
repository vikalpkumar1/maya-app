package com.vikalpai.maya.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Downscales a picked image before sending it to Gemini's vision endpoint —
 * keeps uploads fast and light on mobile data instead of sending a full-res
 * multi-MB photo for something the model only needs at modest resolution.
 */
object ImageHelper {
    private const val MAX_DIMENSION = 1024

    fun encodeForUpload(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val scale = minOf(
                MAX_DIMENSION.toFloat() / original.width,
                MAX_DIMENSION.toFloat() / original.height,
                1f
            )
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                )
            } else original

            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
