package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM pdf_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<LocalPdfDocument>>

    @Query("SELECT * FROM pdf_documents WHERE id = :documentId LIMIT 1")
    suspend fun getDocumentById(documentId: Int): LocalPdfDocument?

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY sequenceOrder ASC")
    fun getPagesForDocument(documentId: Int): Flow<List<PageEntity>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY sequenceOrder ASC")
    suspend fun getPagesForDocumentSync(documentId: Int): List<PageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: LocalPdfDocument): Long

    @Update
    suspend fun updateDocument(doc: LocalPdfDocument)

    @Delete
    suspend fun deleteDocument(doc: LocalPdfDocument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity): Long

    @Update
    suspend fun updatePage(page: PageEntity)

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Int)

    @Transaction
    suspend fun deleteDocumentAndPages(doc: LocalPdfDocument) {
        deletePagesForDocument(doc.id)
        deleteDocument(doc)
    }

    @Transaction
    suspend fun reorderPages(pages: List<PageEntity>) {
        pages.forEachIndexed { index, page ->
            updatePage(page.copy(sequenceOrder = index))
        }
    }
}
