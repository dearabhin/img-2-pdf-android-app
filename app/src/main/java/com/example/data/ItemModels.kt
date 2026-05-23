package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class LocalPdfDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncProvider: String = "None",
    val syncTime: Long = 0L,
    val passcode: String? = null // For securing individual documents in the app vault
)

@Entity(tableName = "document_pages")
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val imageUri: String, // Can be a system URI or a sample asset identifier
    val filterName: String = "Original", // Original, Grayscale, DocScan, Sepia, Warm, Cool
    val sequenceOrder: Int
)
