package com.example.data

import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<LocalPdfDocument>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(documentId: Int): LocalPdfDocument? {
        return documentDao.getDocumentById(documentId)
    }

    fun getPagesForDocument(documentId: Int): Flow<List<PageEntity>> {
        return documentDao.getPagesForDocument(documentId)
    }

    suspend fun getPagesForDocumentSync(documentId: Int): List<PageEntity> {
        return documentDao.getPagesForDocumentSync(documentId)
    }

    suspend fun insertDocument(document: LocalPdfDocument): Int {
        return documentDao.insertDocument(document).toInt()
    }

    suspend fun updateDocument(document: LocalPdfDocument) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocument(document: LocalPdfDocument) {
        documentDao.deleteDocumentAndPages(document)
    }

    suspend fun insertPage(page: PageEntity): Int {
        return documentDao.insertPage(page).toInt()
    }

    suspend fun updatePage(page: PageEntity) {
        documentDao.updatePage(page)
    }

    suspend fun deletePage(page: PageEntity) {
        documentDao.deletePage(page)
    }

    suspend fun reorderPages(pages: List<PageEntity>) {
        documentDao.reorderPages(pages)
    }
}
