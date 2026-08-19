package com.pico.swan.focusbloom.platform

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.MediaStore
import android.view.View

class FocusBloomScreenshotExporter(
    private val context: Context,
    private val view: View,
) {
    fun export(): Result<Unit> = runCatching {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "focus-bloom-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FocusBloom")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create screenshot destination")
        context.contentResolver.openOutputStream(uri)?.use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Screenshot encoding failed" }
        } ?: error("Unable to open screenshot destination")
        bitmap.recycle()
    }
}
