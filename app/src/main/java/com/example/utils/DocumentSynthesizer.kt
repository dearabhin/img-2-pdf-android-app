package com.example.utils

import android.graphics.*
import java.io.File
import java.io.FileOutputStream

object DocumentSynthesizer {

    fun generateSampleBitmap(type: String): Bitmap {
        val width = 600
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clear white page background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw a neat subtle textured light-gray paper borders
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat(), borderPaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        when (type) {
            "invoice" -> {
                // Main Header Banner
                val headerPaint = Paint().apply {
                    color = Color.parseColor("#1E3A8A") // Premium Deep Blue
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(10f, 10f, (width - 10).toFloat(), 150f, headerPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 32f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("TAX INVOICE", 40f, 75f, textPaint)

                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("Invoice: #INV-2026-081", 40f, 115f, textPaint)
                canvas.drawText("Date: May 23, 2026", (width - 220).toFloat(), 75f, textPaint)
                canvas.drawText("Due: June 23, 2026", (width - 220).toFloat(), 115f, textPaint)

                // Subtitle/Addresses
                textPaint.color = Color.BLACK
                textPaint.textSize = 16f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("Billed To:", 40f, 210f, textPaint)
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("Google AI Studio Build User", 40f, 235f, textPaint)
                canvas.drawText("Enterprise Workspace Suite", 40f, 255f, textPaint)

                canvas.drawText("Issued From:", (width - 240).toFloat(), 210f, textPaint)
                canvas.drawText("Applet Software Corp.", (width - 240).toFloat(), 235f, textPaint)
                canvas.drawText("Cloud Sandbox Hub-81", (width - 240).toFloat(), 255f, textPaint)

                // Table Header
                val tableHeaderPaint = Paint().apply {
                    color = Color.parseColor("#F3F4F6") // Mild Grey
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(40f, 300f, (width - 40).toFloat(), 340f, tableHeaderPaint)

                val linePaint = Paint().apply {
                    color = Color.DKGRAY
                    strokeWidth = 2f
                }
                canvas.drawLine(40f, 340f, (width - 40).toFloat(), 340f, linePaint)

                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("Item Details", 60f, 325f, textPaint)
                canvas.drawText("Qty", 380f, 325f, textPaint)
                canvas.drawText("Amount", (width - 140).toFloat(), 325f, textPaint)

                // Line Items
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                var currentY = 385f
                val items = listOf(
                    Triple("High-Density Image OCR Parse", "1", "$750.00"),
                    Triple("Secure AES Cloud Storage Safe", "12", "$240.00"),
                    Triple("Multi-Doc Batch Compiler Node", "3", "$450.00"),
                    Triple("PDF Format Compression Core", "1", "$110.00")
                )

                for (item in items) {
                    canvas.drawText(item.first, 60f, currentY, textPaint)
                    canvas.drawText(item.second, 385f, currentY, textPaint)
                    canvas.drawText(item.third, (width - 140).toFloat(), currentY, textPaint)
                    canvas.drawLine(40f, currentY + 15f, (width - 40).toFloat(), currentY + 15f, borderPaint)
                    currentY += 50f
                }

                // Totals
                textPaint.textSize = 15f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("Subtotal:", 300f, 620f, textPaint)
                canvas.drawText("$1,550.00", (width - 140).toFloat(), 620f, textPaint)

                canvas.drawText("Tax (10%):", 300f, 650f, textPaint)
                canvas.drawText("$155.00", (width - 140).toFloat(), 650f, textPaint)

                // Total Highlight
                textPaint.textSize = 18f
                textPaint.color = Color.parseColor("#1E3A8A")
                canvas.drawText("Total Due USD:", 250f, 700f, textPaint)
                canvas.drawText("$1,705.00", (width - 140).toFloat(), 700f, textPaint)

                // Footer
                textPaint.color = Color.GRAY
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                canvas.drawText("Payment instructions securely encapsulated within cloud registry. Thank you!", 100f, 760f, textPaint)
            }
            "receipt" -> {
                // Receipt Header
                textPaint.textSize = 28f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                canvas.drawText("COSMO SUPERMARKET", 120f, 80f, textPaint)

                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                canvas.drawText("77 Galaxy Blvd, Nebulae Sector", 170f, 110f, textPaint)
                canvas.drawText("Phone: (555) 789-0123", 200f, 130f, textPaint)
                canvas.drawText("ST# 9821 OP# 0004 TE# 02 TR# 9987A", 120f, 155f, textPaint)

                val dashPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
                }
                canvas.drawLine(40f, 180f, (width - 40).toFloat(), 180f, dashPaint)

                // Items list
                var itemY = 220f
                val receiptItems = listOf(
                    "Organic Avocados (3x)" to "6.99",
                    "A4 Laser Copy Paper x500" to "12.50",
                    "Premium Coffee Beans 1kg" to "18.99",
                    "Fresh Sourdough Bread" to "4.25",
                    "Acoustic Synth Strings" to "14.00",
                    "Double Chocolate Cookies" to "5.45",
                    "Sparkling Water Recyclable x6" to "8.90"
                )

                for (ri in receiptItems) {
                    canvas.drawText(ri.first, 50f, itemY, textPaint)
                    val amtWidth = textPaint.measureText(ri.second)
                    canvas.drawText(ri.second, width - 50f - amtWidth, itemY, textPaint)
                    itemY += 35f
                }

                canvas.drawLine(40f, itemY, (width - 40).toFloat(), itemY, dashPaint)
                itemY += 40f

                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                canvas.drawText("SUBTOTAL", 50f, itemY, textPaint)
                var text = "71.08"
                canvas.drawText(text, width - 50f - textPaint.measureText(text), itemY, textPaint)

                itemY += 35f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                canvas.drawText("SALES TAX (8.25%)", 50f, itemY, textPaint)
                text = "5.86"
                canvas.drawText(text, width - 50f - textPaint.measureText(text), itemY, textPaint)

                itemY += 45f
                textPaint.textSize = 18f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                canvas.drawText("TOTAL COST", 50f, itemY, textPaint)
                text = "$76.94"
                canvas.drawText(text, width - 50f - textPaint.measureText(text), itemY, textPaint)

                itemY += 45f
                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                canvas.drawText("PAID VIA: SECURE SMART-PAY", 50f, itemY, textPaint)
                canvas.drawText("AUTH: *******8821", 50f, itemY + 25f, textPaint)

                // Barcode simulation
                val barcodeY = itemY + 70f
                val barcodePaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                var xOffset = 150f
                val barWidths = listOf(4, 2, 8, 4, 2, 6, 8, 2, 4, 4, 8, 2, 6, 4, 8, 4, 2, 8, 4, 2, 6)
                for (bw in barWidths) {
                    canvas.drawRect(xOffset, barcodeY, xOffset + bw, barcodeY + 60f, barcodePaint)
                    xOffset += bw + 3f
                }

                canvas.drawText("90281-08153-2901-7694", 190f, barcodeY + 85f, textPaint)
            }
            "photo" -> {
                // Dynamic Landscape/Photo Simulation
                // Header Label in the borders
                val namePaint = Paint().apply {
                    color = Color.parseColor("#EC4899") // Hot Pink
                    textSize = 24f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("Scenic Explorer Portfolio", 120f, 60f, namePaint)

                // Background photo canvas panel
                val photoRect = RectF(50f, 100f, (width - 50).toFloat(), 650f)
                val photoBgPaint = Paint().apply {
                    shader = LinearGradient(0f, 100f, 0f, 650f,
                        Color.parseColor("#111827"), // Slate Dark
                        Color.parseColor("#4C1D95"), // Deep Purple
                        Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(photoRect, photoBgPaint)

                // Draw Sun
                val sunPaint = Paint().apply {
                    shader = RadialGradient((width / 2).toFloat(), 380f, 140f,
                        intArrayOf(Color.parseColor("#FDE047"), Color.parseColor("#F97316"), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.4f, 1.0f), Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle((width / 2).toFloat(), 380f, 140f, sunPaint)

                // Draw Mountain silhouettes
                val path = Path().apply {
                    moveTo(50f, 650f)
                    lineTo(200f, 420f)
                    lineTo(350f, 530f)
                    lineTo(500f, 380f)
                    lineTo((width - 50).toFloat(), 650f)
                    close()
                }
                val mtnPaint = Paint().apply {
                    color = Color.parseColor("#1F1A3A")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawPath(path, mtnPaint)

                // Fore sunset waves/mountains
                val forePath = Path().apply {
                    moveTo(50f, 650f)
                    lineTo(120f, 500f)
                    lineTo(280f, 580f)
                    lineTo(440f, 480f)
                    lineTo((width - 50).toFloat(), 650f)
                    close()
                }
                val foreMtnPaint = Paint().apply {
                    color = Color.parseColor("#0C061C")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawPath(forePath, foreMtnPaint)

                // Drawing Photo Frame Title
                textPaint.textSize = 14f
                textPaint.color = Color.GRAY
                canvas.drawText("Shot on secure 108MP camera node. ISO 100, f/1.8", 120f, 685f, textPaint)
                canvas.drawText("Digital Watermark Secured in App Sandbox Storage", 120f, 715f, textPaint)
            }
            "idcard" -> {
                // Identification Badge / Card sample representation
                val brandPaint = Paint().apply {
                    color = Color.parseColor("#059669") // Forest Emerald Green
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(30f, 30f, (width - 30).toFloat(), 150f, brandPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 28f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("SECURE ACCESS ID", 80f, 85f, textPaint)
                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("LEVEL 4 SPECIALIST clearance PASS", 80f, 120f, textPaint)

                // Face Photo ID Frame
                val framePaint = Paint().apply {
                    color = Color.parseColor("#059 green") // Oh, hex colors are better
                    color = Color.GRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(80f, 210f, 240f, 410f, framePaint)

                // Draw simulated security portrait / outline avatar
                val avatarPaint = Paint().apply {
                    color = Color.parseColor("#374151")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                // Head
                canvas.drawCircle(160f, 275f, 45f, avatarPaint)
                // Shoulders
                val shoulders = Path().apply {
                    moveTo(95f, 410f)
                    quadTo(160f, 330f, 225f, 410f)
                    close()
                }
                canvas.drawPath(shoulders, avatarPaint)

                // Details alongside the portrait
                textPaint.color = Color.BLACK
                textPaint.textSize = 18f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("HOLDER DETAILS:", 270f, 230f, textPaint)

                textPaint.textSize = 15f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("Name: ARCHITECT SMITH", 270f, 270f, textPaint)
                canvas.drawText("Serial KEY: US-9281-X", 270f, 305f, textPaint)
                canvas.drawText("Division: APPS SYSTEMS", 270f, 340f, textPaint)
                canvas.drawText("Expires: DEC 31, 2029", 270f, 375f, textPaint)

                // Divider line
                canvas.drawLine(80f, 450f, (width - 80).toFloat(), 450f, framePaint)

                // Security barcode
                val codePaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                var codeX = 80f
                for (b in listOf(10, 5, 15, 5, 20, 5, 10, 15, 5, 12, 18, 5, 10, 15, 20, 5, 10, 5)) {
                    canvas.drawRect(codeX, 480f, codeX + b, 540f, codePaint)
                    codeX += b + 4f
                }
                textPaint.textSize = 14f
                canvas.drawText("RFID EMULATED: 9281-A81-992-K", 80f, 570f, textPaint)

                // Warning badge
                val warnPaint = Paint().apply {
                    color = Color.parseColor("#FEF2F2") // Soft Red
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(80f, 620f, (width - 80).toFloat(), 740f, warnPaint)

                val warnBorder = Paint().apply {
                    color = Color.parseColor("#EF4444") // Red Border
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                }
                canvas.drawRect(80f, 620f, (width - 80).toFloat(), 740f, warnBorder)

                textPaint.color = Color.parseColor("#991B1B") // Dark Red text
                textPaint.textSize = 13f
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("SECURITY WARNING / TERMS / CONDITIONS", 95f, 650f, textPaint)

                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textPaint.textSize = 11f
                canvas.drawText("This credentials card remains the sole property of security corp.", 95f, 680f, textPaint)
                canvas.drawText("Unauthorized duplication of this image document is strictly audited.", 95f, 700f, textPaint)
                canvas.drawText("Loss must be report immediate via cloud vault protocol.", 95f, 720f, textPaint)
            }
        }

        return bitmap
    }

    // Save sample bitmaps to a cache directory and return their Absolute Path / URI string
    fun saveSampleBitmapToCache(context: android.content.Context, type: String): String {
        val bitmap = generateSampleBitmap(type)
        val directory = File(context.cacheDir, "sample_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "sample_$type.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }
}
