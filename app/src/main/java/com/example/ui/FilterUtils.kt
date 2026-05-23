package com.example.ui

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

object FilterUtils {

    val GrayscaleMatrix = ColorMatrix(floatArrayOf(
        0.213f, 0.715f, 0.072f, 0f, 0f,
        0.213f, 0.715f, 0.072f, 0f, 0f,
        0.213f, 0.715f, 0.072f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    ))

    val SepiaMatrix = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    ))

    // Drastic contrast, clipping near-white to crisp white, boosting text
    val DocScanMatrix = ColorMatrix(floatArrayOf(
        1.6f, 0f, 0f, 0f, -40f,
        0f, 1.6f, 0f, 0f, -40f,
        0f, 0f, 1.6f, 0f, -40f,
        0f, 0f, 0f, 1f, 0f
    ))

    val WarmMatrix = ColorMatrix(floatArrayOf(
        1.15f, 0f, 0f, 0f, 10f,
        0f, 1.0f, 0f, 0f, 10f,
        0f, 0f, 0.85f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    val CoolMatrix = ColorMatrix(floatArrayOf(
        0.85f, 0f, 0f, 0f, 0f,
        0f, 1.0f, 0f, 0f, 10f,
        0f, 0f, 1.2f, 0f, 15f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun getColorMatrix(filterName: String): ColorMatrix? {
        return when (filterName) {
            "Grayscale" -> GrayscaleMatrix
            "DocScan" -> DocScanMatrix
            "Sepia" -> SepiaMatrix
            "Warm" -> WarmMatrix
            "Cool" -> CoolMatrix
            else -> null
        }
    }

    fun getColorFilter(filterName: String): ColorFilter? {
        val matrix = getColorMatrix(filterName) ?: return null
        return ColorFilter.colorMatrix(matrix)
    }

    // Convert Compose ColorMatrix to android.graphics.ColorMatrix for standard bitmaps
    fun toAndroidGraphicsColorMatrix(filterName: String): android.graphics.ColorMatrix? {
        val composeMatrix = getColorMatrix(filterName) ?: return null
        return android.graphics.ColorMatrix(composeMatrix.values)
    }
}
