package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import com.example.data.LocalPdfDocument
import com.example.data.PageEntity
import com.example.ui.FilterUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfCompiler {

    /**
     * Compiles a LocalPdfDocument and its Pages into a physical PDF file.
     * Applies the correct page filters to the images during compilation!
     * Returns the compiled File path.
     */
    fun compileToPdf(
        context: Context,
        docInfo: LocalPdfDocument,
        pages: List<PageEntity>
    ): File? {
        if (pages.isEmpty()) return null

        val pdfDoc = PdfDocument()

        try {
            // A4 page dimensions in PostScript points (72 points/inch)
            val reqWidth = 595
            val reqHeight = 842

            for ((index, page) in pages.sortedBy { it.sequenceOrder }.withIndex()) {
                val originalBitmap = loadBitmap(context, page.imageUri) ?: continue

                // Check and apply filter to the bitmap
                val filteredBitmap = applyFilterToBitmap(originalBitmap, page.filterName)

                // Create PDF page description
                val pageInfo = PdfDocument.PageInfo.Builder(reqWidth, reqHeight, index + 1).create()
                val pdfPage = pdfDoc.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Clear background with white color
                canvas.drawColor(android.graphics.Color.WHITE)

                // Draw bitmap scaling to fit the A4 page aspect ratio elegantly with 20pt margins
                val margin = 20
                val targetRect = calculateFitRect(
                    filteredBitmap.width,
                    filteredBitmap.height,
                    reqWidth - (margin * 2),
                    reqHeight - (margin * 2)
                )

                // Offset by margin
                targetRect.offset(margin, margin)

                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }

                canvas.drawBitmap(filteredBitmap, null, targetRect, paint)

                pdfDoc.finishPage(pdfPage)

                // Recycle temporary filtered bitmap if copy was made
                if (filteredBitmap != originalBitmap) {
                    filteredBitmap.recycle()
                }
                originalBitmap.recycle()
            }

            // Create output directory for compiled files
            val outputDir = File(context.filesDir, "compiled_pdfs")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Make safe file name from document title
            val sanitizedTitle = docInfo.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val pdfFile = File(outputDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { fos ->
                pdfDoc.writeTo(fos)
            }

            return pdfFile

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                pdfDoc.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun loadBitmap(context: Context, uriOrPath: String): Bitmap? {
        return try {
            if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                val uri = Uri.parse(uriOrPath)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                inputStream?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                val file = File(uriOrPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyFilterToBitmap(bitmap: Bitmap, filterName: String): Bitmap {
        val androidMatrix = FilterUtils.toAndroidGraphicsColorMatrix(filterName) ?: return bitmap

        // Create a new bitmap to write filtered values
        val filtered = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filtered)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(androidMatrix)
            isAntiAlias = true
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return filtered
    }

    private fun calculateFitRect(srcW: Int, srcH: Int, destW: Int, destH: Int): Rect {
        val srcRatio = srcW.toFloat() / srcH
        val destRatio = destW.toFloat() / destH

        return if (srcRatio > destRatio) {
            // Width is the limiting factor
            val finalW = destW
            val finalH = (destW / srcRatio).toInt()
            val top = (destH - finalH) / 2
            Rect(0, top, finalW, top + finalH)
        } else {
            // Height is the limiting factor
            val finalH = destH
            val finalW = (destH * srcRatio).toInt()
            val left = (destW - finalW) / 2
            Rect(left, 0, left + finalW, finalH)
        }
    }
}
