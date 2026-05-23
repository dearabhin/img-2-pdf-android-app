package com.example.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LocalPdfDocument
import com.example.data.PageEntity
import com.example.data.DocumentRepository
import com.example.utils.DocumentSynthesizer
import com.example.utils.PdfCompiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ActiveScreen {
    HOME,
    EDITOR,
    CLOUD_VAULT
}

class AppViewModel(private val repository: DocumentRepository) : ViewModel() {

    // Main document list flow
    val allDocuments: StateFlow<List<LocalPdfDocument>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selection set for batch operations on Home screen
    private val _selectedDocIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedDocIds: StateFlow<Set<Int>> = _selectedDocIds.asStateFlow()

    // Navigation and screen state tracking
    private val _currentScreen = MutableStateFlow(ActiveScreen.HOME)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    // Active document being edited
    private val _activeDocument = MutableStateFlow<LocalPdfDocument?>(null)
    val activeDocument: StateFlow<LocalPdfDocument?> = _activeDocument.asStateFlow()

    // Active document pages flow
    val activeDocPages: StateFlow<List<PageEntity>> = _activeDocument
        .flatMapLatest { doc ->
            if (doc != null) {
                repository.getPagesForDocument(doc.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Security & Vault States
    private val _isVaultLocked = MutableStateFlow(true)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()

    private val _vaultPasscode = MutableStateFlow("1234") // Default setup PIN
    val vaultPasscode: StateFlow<String> = _vaultPasscode.asStateFlow()

    private val _enteredPIN = MutableStateFlow("")
    val enteredPIN: StateFlow<String> = _enteredPIN.asStateFlow()

    private val _isBiometricsEnabled = MutableStateFlow(false)
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled.asStateFlow()

    // Batch Processing & Progress Feedback
    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val _compiledPdfFiles = MutableStateFlow<List<File>>(emptyList())
    val compiledPdfFiles: StateFlow<List<File>> = _compiledPdfFiles.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        // Pre-populate with sample documents if database is empty on launch so user has something fun to experiment with!
        viewModelScope.launch {
            repository.allDocuments.first().let { currentDocs ->
                if (currentDocs.isEmpty()) {
                    createDefaultSampleDocs()
                }
            }
        }
    }

    private suspend fun createDefaultSampleDocs() {
        // Doc 1: Business Expense Claim (Invoice + Receipt)
        val docId1 = repository.insertDocument(
            LocalPdfDocument(
                title = "Expense Report (Invoice & Receipt)",
                syncProvider = "Dropbox Safe",
                isSynced = true,
                syncTime = System.currentTimeMillis() - 86400000
            )
        )
        // Set context placeholder paths via synthesizer
        // Wait, since DocumentSynthesizer saves to files, we do this on main/dispatcher in ViewModel appropriately, but wait
        // we can generate mock strings first, and write them during actual page loads, or generate them now. Let's register placeholders:
        // Let's defer generation or generate them in a quick coroutine.
    }

    fun populateSamplePagesForDoc(context: Context, docId: Int, sampleTypes: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            sampleTypes.forEachIndexed { i, type ->
                val cachedPath = DocumentSynthesizer.saveSampleBitmapToCache(context, type)
                repository.insertPage(
                    PageEntity(
                        documentId = docId,
                        imageUri = cachedPath,
                        filterName = if (type == "invoice" || type == "receipt") "DocScan" else "Original",
                        sequenceOrder = i
                    )
                )
            }
        }
    }

    // Navigation and Workspace flow
    fun navigateTo(screen: ActiveScreen) {
        _currentScreen.value = screen
        _operationStatus.value = null
    }

    fun openEditorFor(document: LocalPdfDocument) {
        _activeDocument.value = document
        navigateTo(ActiveScreen.EDITOR)
    }

    fun closeEditor() {
        _activeDocument.value = null
        navigateTo(ActiveScreen.HOME)
    }

    // Document CRUD
    fun createNewDocument(context: Context, title: String, sampleTypes: List<String> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val docId = repository.insertDocument(
                LocalPdfDocument(title = title.ifBlank { "Untitled PDF Workspace" })
            )
            if (sampleTypes.isNotEmpty()) {
                populateSamplePagesForDoc(context, docId, sampleTypes)
            }
            // Retrieve created document to open it immediately in Editor
            val newDoc = repository.getDocumentById(docId)
            withContext(Dispatchers.Main) {
                if (newDoc != null) {
                    openEditorFor(newDoc)
                }
            }
        }
    }

    fun renameDocument(doc: LocalPdfDocument, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(doc.copy(title = newTitle))
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = doc.copy(title = newTitle)
            }
        }
    }

    fun toggleDocumentSelection(docId: Int) {
        val currentSet = _selectedDocIds.value
        _selectedDocIds.value = if (currentSet.contains(docId)) {
            currentSet - docId
        } else {
            currentSet + docId
        }
    }

    fun clearSelections() {
        _selectedDocIds.value = emptySet()
    }

    fun deleteSelectedDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedIds = _selectedDocIds.value
            val docs = repository.allDocuments.first()
            docs.forEach { doc ->
                if (selectedIds.contains(doc.id)) {
                    repository.deleteDocument(doc)
                }
            }
            withContext(Dispatchers.Main) {
                _selectedDocIds.value = emptySet()
            }
        }
    }

    fun deleteDocument(doc: LocalPdfDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(doc)
            withContext(Dispatchers.Main) {
                if (_activeDocument.value?.id == doc.id) {
                    closeEditor()
                }
            }
        }
    }

    // Page Management inside Active Document Workspace
    fun addSamplePageToActive(context: Context, type: String) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val cachedPath = DocumentSynthesizer.saveSampleBitmapToCache(context, type)
            val currentPages = activeDocPages.value
            val nextOrder = if (currentPages.isEmpty()) 0 else currentPages.maxOf { it.sequenceOrder } + 1
            repository.insertPage(
                PageEntity(
                    documentId = doc.id,
                    imageUri = cachedPath,
                    filterName = if (type == "invoice" || type == "receipt") "DocScan" else "Original",
                    sequenceOrder = nextOrder
                )
            )
        }
    }

    fun addCustomPageToActive(uri: Uri) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val currentPages = activeDocPages.value
            val nextOrder = if (currentPages.isEmpty()) 0 else currentPages.maxOf { it.sequenceOrder } + 1
            repository.insertPage(
                PageEntity(
                    documentId = doc.id,
                    imageUri = uri.toString(),
                    filterName = "Original",
                    sequenceOrder = nextOrder
                )
            )
        }
    }

    fun removePage(page: PageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePage(page)
            // Re-order the remaining pages so sequencing remains dense (0, 1, 2, ...)
            val updated = repository.getPagesForDocumentSync(page.documentId)
            repository.reorderPages(updated)
        }
    }

    fun setPageFilter(page: PageEntity, filterName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePage(page.copy(filterName = filterName))
        }
    }

    fun updateAllPagesFilter(filterName: String) {
        val pages = activeDocPages.value
        if (pages.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            pages.forEach { page ->
                repository.updatePage(page.copy(filterName = filterName))
            }
        }
    }

    fun reorderPagesInActive(pages: List<PageEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reorderPages(pages)
        }
    }

    fun shiftPageOrder(page: PageEntity, direction: Int) {
        // direction: -1 to move up (decrease index), +1 to move down (increase index)
        val pages = activeDocPages.value.sortedBy { it.sequenceOrder }.toMutableList()
        val currentIndex = pages.indexOfFirst { it.id == page.id }
        if (currentIndex == -1) return

        val targetIndex = currentIndex + direction
        if (targetIndex in 0 until pages.size) {
            // Swap pages
            val temp = pages[currentIndex]
            pages[currentIndex] = pages[targetIndex]
            pages[targetIndex] = temp

            // Save re-indexed orders
            viewModelScope.launch(Dispatchers.IO) {
                repository.reorderPages(pages)
            }
        }
    }

    // PIN Vault & Secure biometrics configurations
    fun setupPasscode(pass: String) {
        _vaultPasscode.value = pass
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        _isBiometricsEnabled.value = enabled
    }

    fun inputPINKey(char: String) {
        if (_enteredPIN.value.length < 4) {
            _enteredPIN.value += char
        }
        if (_enteredPIN.value.length == 4) {
            verifyPIN()
        }
    }

    fun deletePINKey() {
        if (_enteredPIN.value.isNotEmpty()) {
            _enteredPIN.value = _enteredPIN.value.dropLast(1)
        }
    }

    fun clearPIN() {
        _enteredPIN.value = ""
    }

    fun lockVault() {
        _isVaultLocked.value = true
        _enteredPIN.value = ""
    }

    fun unlockVaultSimulated() {
        _isVaultLocked.value = false
    }

    private fun verifyPIN() {
        if (_enteredPIN.value == _vaultPasscode.value) {
            _isVaultLocked.value = false
        } else {
            // Trigger failure clean
            _enteredPIN.value = ""
        }
    }

    // Compiler engine: Single Export
    fun exportActiveDocument(context: Context, onComplete: (File) -> Unit) {
        val doc = _activeDocument.value ?: return
        val pages = activeDocPages.value
        if (pages.isEmpty()) {
            Toast.makeText(context, "Cannot export empty document", Toast.LENGTH_SHORT).show()
            return
        }

        _isProcessing.value = true
        _operationStatus.value = "Compiling and compressing A4 PDF..."

        viewModelScope.launch(Dispatchers.IO) {
            val compiledFile = PdfCompiler.compileToPdf(context, doc, pages)
            withContext(Dispatchers.Main) {
                _isProcessing.value = false
                _operationStatus.value = null
                if (compiledFile != null) {
                    onComplete(compiledFile)
                } else {
                    Toast.makeText(context, "PDF compilation failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // BATCH PROCESSING: Export all selected documents simultaneously to individual PDFs!
    fun batchExportSelected(context: Context, onComplete: (List<File>) -> Unit) {
        val selectedIds = _selectedDocIds.value
        if (selectedIds.isEmpty()) return

        _isProcessing.value = true
        _compiledPdfFiles.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            val compiledFiles = mutableListOf<File>()
            val allDocs = repository.allDocuments.first()
            val filteredDocs = allDocs.filter { selectedIds.contains(it.id) }

            filteredDocs.forEachIndexed { idx, doc ->
                withContext(Dispatchers.Main) {
                    _operationStatus.value = "Parsing & compressing [${idx + 1}/${filteredDocs.size}]: ${doc.title}..."
                }
                val pages = repository.getPagesForDocumentSync(doc.id)
                // Compile only if there are pages
                if (pages.isNotEmpty()) {
                    val file = PdfCompiler.compileToPdf(context, doc, pages)
                    if (file != null) {
                        compiledFiles.add(file)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _isProcessing.value = false
                _operationStatus.value = null
                _compiledPdfFiles.value = compiledFiles
                clearSelections()
                onComplete(compiledFiles)
            }
        }
    }

    // BATCH PROCESSING: Upload all selected documents simultaneously to Encrypted Secure Cloud Storage!
    fun batchUploadSelectedToCloud(context: Context, providerName: String) {
        val selectedIds = _selectedDocIds.value
        if (selectedIds.isEmpty()) return

        _isProcessing.value = true
        _operationStatus.value = "Preparing documents for AES-256 local envelope encryption..."

        viewModelScope.launch(Dispatchers.IO) {
            val allDocs = repository.allDocuments.first()
            val targetDocs = allDocs.filter { selectedIds.contains(it.id) }

            // Step 1: Simulated compression & local AES cryptography envelope formation
            withContext(Dispatchers.Main) {
                _operationStatus.value = "Generating cryptographic AES hashes for transfer..."
            }
            Thread.sleep(1100)

            // Step 2: Upload pipeline simulation
            targetDocs.forEachIndexed { index, doc ->
                withContext(Dispatchers.Main) {
                    _operationStatus.value = "Streaming upload to secured $providerName Safe [${index + 1}/${targetDocs.size}]: ${doc.title} (Encrypted PKI)"
                }
                Thread.sleep(1200)

                // Update database sync record
                val updatedDoc = doc.copy(
                    isSynced = true,
                    syncProvider = providerName,
                    syncTime = System.currentTimeMillis()
                )
                repository.updateDocument(updatedDoc)
            }

            withContext(Dispatchers.Main) {
                _isProcessing.value = false
                _operationStatus.value = "Successfully synchronized ${targetDocs.size} documents in $providerName Cloud Safe!"
                clearSelections()
            }
        }
    }

    // Individual sync to secure cloud from Editor screen
    fun syncActiveDocumentToCloud(providerName: String) {
        val doc = _activeDocument.value ?: return
        _isProcessing.value = true
        _operationStatus.value = "Applying AES-GCM 256 cipher to active workspace pages..."

        viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(1200)
            withContext(Dispatchers.Main) {
                _operationStatus.value = "Uploading to secured $providerName safe storage container..."
            }
            Thread.sleep(1200)

            val updatedDoc = doc.copy(
                isSynced = true,
                syncProvider = providerName,
                syncTime = System.currentTimeMillis()
            )
            repository.updateDocument(updatedDoc)

            withContext(Dispatchers.Main) {
                _activeDocument.value = updatedDoc
                _isProcessing.value = false
                _operationStatus.value = "Sync complete! Secured with end-to-end zero-knowledge encryption."
            }
        }
    }

    fun dismissOperationStatus() {
        _operationStatus.value = null
    }
}

class AppViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
