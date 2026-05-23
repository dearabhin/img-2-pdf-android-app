package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.LocalPdfDocument
import com.example.data.PageEntity
import com.example.ui.ActiveScreen
import com.example.ui.AppViewModel
import com.example.ui.FilterUtils
import com.example.utils.DocumentSynthesizer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeDoc by viewModel.activeDocument.collectAsStateWithLifecycle()
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val operationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    // File Sharing Trigger Dialog
    var showExportDialog by remember { mutableStateOf<File?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(300),
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        ActiveScreen.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onExportSuccess = { file -> showExportDialog = file }
                        )
                        ActiveScreen.EDITOR -> EditorScreen(
                            viewModel = viewModel,
                            onExportSuccess = { file -> showExportDialog = file }
                        )
                        ActiveScreen.CLOUD_VAULT -> {
                            if (isVaultLocked) {
                                VaultSecurityScreen(viewModel = viewModel)
                            } else {
                                CloudVaultDashboard(viewModel = viewModel)
                            }
                        }
                    }
                }
            }

            // Global Navigation Bar (Hidden when editing a document workspace to maximize design canvasing)
            if (activeDoc == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentScreen == ActiveScreen.HOME,
                        onClick = { viewModel.navigateTo(ActiveScreen.HOME) },
                        icon = { Icon(Icons.Filled.Folder, contentDescription = "Documents") },
                        label = { Text("Vault Files", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == ActiveScreen.CLOUD_VAULT,
                        onClick = { viewModel.navigateTo(ActiveScreen.CLOUD_VAULT) },
                        icon = {
                            Icon(
                                if (isVaultLocked) Icons.Filled.Lock else Icons.Filled.CloudUpload,
                                contentDescription = "Cloud Storage"
                            )
                        },
                        label = { Text("Secure Cloud", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_vault")
                    )
                }
            }
        }

        // Global Transparent Processing Modal Overlay (Stops inputs during heavy cryptographic operation or PDF compilation)
        if (isProcessing || operationStatus != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {}, // Scrim blocking
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = operationStatus ?: "Processing payload...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AES-Encrypted Sandbox Pipe",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Export Dialog
        showExportDialog?.let { file ->
            Dialog(onDismissRequest = { showExportDialog = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PDF Generated Successfully!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size: %.2f KB".format(file.length() / 1024.0),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                sharePdfFile(context, file)
                                showExportDialog = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_pdf_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share / Export PDF")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showExportDialog = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

// ------ HOME SCREEN ------
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onExportSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val docs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val selectedDocIds by viewModel.selectedDocIds.collectAsStateWithLifecycle()
    
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { /* Menu placeholder */ }) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menu drawer",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "PDF Studio",
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Document PDF Safe • AES-256",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.CLOUD_VAULT) }) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = "Shield Security status",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // User Avatar pill JD
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (selectedDocIds.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Filled.Add, "New") },
                    text = { Text("New Workspace") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("create_doc_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (docs.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.InsertDriveFile,
                            contentDescription = "No receipts folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Encrypted Vault is Empty",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a document workspace, import files, apply advanced scanning visual filters, and compile PDFs locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_empty_state_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instant New Workspace")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Selection/Batch mode bar
                    AnimatedVisibility(
                        visible = selectedDocIds.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        val batchName = remember(selectedDocIds, docs) {
                            val firstSelected = docs.find { selectedDocIds.contains(it.id) }
                            if (firstSelected != null) {
                                if (selectedDocIds.size > 1) "${firstSelected.title}_and_${selectedDocIds.size - 1}_More" else firstSelected.title
                            } else {
                                "Q3_Financial_Receipts_Final"
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ACTIVE BATCH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${selectedDocIds.size} Files Selected",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.clearSelections() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Clear Selection",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = batchName,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 22.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                viewModel.batchExportSelected(context) { files ->
                                                    if (files.isNotEmpty()) {
                                                        Toast.makeText(
                                                            context,
                                                            "Batch compiled ${files.size} PDFs!",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        onExportSuccess(files.first())
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("batch_compile_btn")
                                        ) {
                                            Icon(
                                                Icons.Filled.PictureAsPdf,
                                                contentDescription = "Batch Compile PDFs",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.batchUploadSelectedToCloud(context, "Dropbox Safe")
                                            },
                                            modifier = Modifier.testTag("batch_backup_btn")
                                        ) {
                                            Icon(
                                                Icons.Filled.CloudUpload,
                                                contentDescription = "Batch Secure Cloud Sync",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSelectedDocuments() },
                                            modifier = Modifier.testTag("batch_delete_btn")
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Batch Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(docs, key = { it.id }) { doc ->
                            val isSelected = selectedDocIds.contains(doc.id)
                            DocumentItemCard(
                                doc = doc,
                                isSelected = isSelected,
                                onSelectToggle = { viewModel.toggleDocumentSelection(doc.id) },
                                onClick = {
                                    if (selectedDocIds.isNotEmpty()) {
                                        viewModel.toggleDocumentSelection(doc.id)
                                    } else {
                                        viewModel.openEditorFor(doc)
                                    }
                                },
                                onDelete = { viewModel.deleteDocument(doc) }
                            )
                        }
                    }
                }
            }
        }

        // Create document Dialog
        if (showCreateDialog) {
            CreateDocumentDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, sampleTypes ->
                    viewModel.createNewDocument(context, name, sampleTypes)
                    showCreateDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentItemCard(
    doc: LocalPdfDocument,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(doc.createdAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(doc.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelectToggle
            )
            .testTag("doc_card_${doc.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual folder icon stack
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSelected) Icons.Filled.Check else Icons.Filled.Description,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (doc.isSynced) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(doc.syncProvider, fontSize = 9.sp)
                                }
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Local Safe Only", fontSize = 9.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = Color.Gray
                            )
                        )
                    }

                    if (doc.passcode != null) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onSelectToggle) {
                    Icon(
                        if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = "Select"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateDocumentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit
) {
    var docName by remember { mutableStateOf("") }
    val templates = listOf(
        "invoice" to "Invoice",
        "receipt" to "Receipt",
        "photo" to "Photo Landscape",
        "idcard" to "Access Credentials"
    )
    val selectedTemplates = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Document Workspace",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text("Workspace Title") },
                    placeholder = { Text("e.g., Weekly Expenses May") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doc_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pre-populate sample pages for instant testing:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { temp ->
                        FilterChip(
                            selected = selectedTemplates.contains(temp.first),
                            onClick = {
                                if (selectedTemplates.contains(temp.first)) {
                                    selectedTemplates.remove(temp.first)
                                } else {
                                    selectedTemplates.add(temp.first)
                                }
                            },
                            label = { Text(temp.second) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onCreate(docName, selectedTemplates.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("dialog_create_btn")
                    ) {
                        Text("Initiate Code-Safe")
                    }
                }
            }
        }
    }
}


// ------ EDITOR SCREEN ------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    viewModel: AppViewModel,
    onExportSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val docInfo by viewModel.activeDocument.collectAsStateWithLifecycle()
    val pages by viewModel.activeDocPages.collectAsStateWithLifecycle()

    var showRenameDialog by remember { mutableStateOf(false) }
    var changeNameText by remember { mutableStateOf("") }

    // Multi page selection logic for bulk filter application / page operations
    val selectedPageIds = remember { mutableStateListOf<Int>() }

    // External image picking launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addCustomPageToActive(uri)
        }
    }

    val doc = docInfo ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeEditor() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            changeNameText = doc.title
                            showRenameDialog = true
                        }
                    ) {
                        Text(
                            text = doc.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncActiveDocumentToCloud("WhisperVault Safe") },
                        modifier = Modifier.testTag("sync_vault_btn")
                    ) {
                        Icon(
                            Icons.Filled.CloudSync,
                            contentDescription = "Secure Backup",
                            tint = if (doc.isSynced) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Floating Editor Controls bar
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bulk Action Filter group
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bulk Filter:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val filters = listOf("Original", "DocScan", "Grayscale")
                        filters.forEach { fName ->
                            SuggestionChip(
                                onClick = { viewModel.updateAllPagesFilter(fName) },
                                label = { Text(fName) },
                                modifier = Modifier.testTag("bulk_filter_$fName")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.exportActiveDocument(context) { file ->
                                onExportSuccess(file)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("compile_pdf_fab")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export A4")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top control bar for adding pages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pages (${pages.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Simulated page injector
                        OutlinedButton(
                            onClick = { viewModel.addSamplePageToActive(context, "invoice") },
                            modifier = Modifier.testTag("add_mock_invoice")
                        ) {
                            Text("+ Instant Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                imagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.testTag("add_custom_page")
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Page", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (pages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No pages in this document wrapper",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap 'Add Page' below or generate mock bills to start constructing your reorderable dynamic PDF.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // List of pages to drag/move/reorder!
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                            PageCardItem(
                                index = index,
                                page = page,
                                totalCount = pages.size,
                                onMoveLeft = { viewModel.shiftPageOrder(page, -1) },
                                onMoveRight = { viewModel.shiftPageOrder(page, 1) },
                                onDelete = { viewModel.removePage(page) },
                                onSetFilter = { filter -> viewModel.setPageFilter(page, filter) }
                            )
                        }
                    }
                }
            }
        }

        // Rename Dialog
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Workspace") },
                text = {
                    OutlinedTextField(
                        value = changeNameText,
                        onValueChange = { changeNameText = it },
                        modifier = Modifier.testTag("rename_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.renameDocument(doc, changeNameText)
                        showRenameDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageCardItem(
    index: Int,
    page: PageEntity,
    totalCount: Int,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
    onSetFilter: (String) -> Unit
) {
    val filters = listOf("Original", "DocScan", "Grayscale", "Sepia", "Warm", "Cool")
    var showFilterMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("page_card_${page.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.05f))
            ) {
                // Image view with dynamic filter applied on overlay! This is incredible and works seamlessly in Compose.
                AsyncImage(
                    model = page.imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = FilterUtils.getColorFilter(page.filterName)
                )

                // Circular Index badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Small filter tag text at bottom right
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topStart = 8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = page.filterName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Delete Overlay button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Remove page",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shifting buttons
                Row {
                    IconButton(
                        onClick = onMoveLeft,
                        enabled = index > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("move_left_${page.id}")
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "Shift Up",
                            tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onMoveRight,
                        enabled = index < totalCount - 1,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("move_right_${page.id}")
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Shift Down",
                            tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                // Set Filter chip button
                Box {
                    AssistChip(
                        onClick = { showFilterMenu = true },
                        label = { Text("Filter", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(12.dp))
                        },
                        modifier = Modifier.height(28.dp)
                    )

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        filters.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter) },
                                onClick = {
                                    onSetFilter(filter)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Sleek style bottom primary indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}


// ------ VAULT SECURITY PIN SCREEN ------
@Composable
fun VaultSecurityScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val enteredPIN by viewModel.enteredPIN.collectAsStateWithLifecycle()
    val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Icon(
            Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Locked Secure Crypt-Vault",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Enter 4-digit master PIN. Default is '1234'",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { idx ->
                val active = enteredPIN.length > idx
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Custom Numeric Pin Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    row.forEach { buttonText ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .background(
                                    color = if (buttonText == "C" || buttonText == "DEL") {
                                        Color.Transparent
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    when (buttonText) {
                                        "DEL" -> viewModel.deletePINKey()
                                        "C" -> viewModel.clearPIN()
                                        else -> viewModel.inputPINKey(buttonText)
                                    }
                                }
                                .testTag("pin_key_$buttonText"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (buttonText == "C" || buttonText == "DEL") {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Simulated fingerprint lock biometrics bypass
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                .clickable {
                    viewModel.unlockVaultSimulated()
                    Toast.makeText(context, "Biometric authentication approved!", Toast.LENGTH_SHORT).show()
                }
                .testTag("biometrics_quick_bypass"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = "Simulate Fingerprint Biometrics Bypass",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Quick Fingerprint Bypass (Touch to Simulate)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}


// ------ SECURE CLOUD STORAGE DASHBOARD ------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudVaultDashboard(viewModel: AppViewModel) {
    val docs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()
    val passcode by viewModel.vaultPasscode.collectAsStateWithLifecycle()

    val totalSynced = docs.count { it.isSynced }
    val totalLocalOnly = docs.count { !it.isSynced }

    var cloudDropboxJoined by remember { mutableStateOf(true) }
    var cloudGoogleJoined by remember { mutableStateOf(false) }
    var whisperVaultJoined by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shield Crypt Encrypted",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your local documents are cryptographically protected on-disk using a multi-layered AES-256 standard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .testTag("lock_vault_now")
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Lock App immediately",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Stats card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Synchronized Storage Register",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Secure Synced Docs",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$totalSynced",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Offline Vault",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$totalLocalOnly",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Secure Cloud Providers list
        item {
            Text(
                text = "Secure Cloud Integration Targets",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            CloudProviderConnectionRow(
                providerName = "Dropbox Enterprise Safe",
                serviceIcon = Icons.Filled.Backup,
                tagLine = "Direct API link with zero-knowledge keys",
                isConnected = cloudDropboxJoined,
                onConnectToggle = { cloudDropboxJoined = it },
                testPref = "dropbox_switch"
            )
        }

        item {
            CloudProviderConnectionRow(
                providerName = "Google Drive Vault Suite",
                serviceIcon = Icons.Filled.DriveFileMove,
                tagLine = "OAuth2 storage compartmentalization",
                isConnected = cloudGoogleJoined,
                onConnectToggle = { cloudGoogleJoined = it },
                testPref = "google_switch"
            )
        }

        item {
            CloudProviderConnectionRow(
                providerName = "WhisperVault Decentralized",
                serviceIcon = Icons.Filled.LockOpen,
                tagLine = "Multi-node cipher sharding",
                isConnected = whisperVaultJoined,
                onConnectToggle = { whisperVaultJoined = it },
                testPref = "whisper_switch"
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Security Configuration Properties",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Biometrics switch
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biometric Lock Bypass",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Unlocks safe vault instantly with fingerprint/face sensors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = isBiometricsEnabled,
                        onCheckedChange = { viewModel.setBiometricsEnabled(it) },
                        modifier = Modifier.testTag("biometrics_toggle")
                    )
                }
            }
        }

        // Current pin passcode identifier list
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Master PIN Code",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Active PIN: $passcode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { viewModel.setupPasscode("8888") },
                        modifier = Modifier.testTag("reset_pin_btn")
                    ) {
                        Text("Instant Reset (8888)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CloudProviderConnectionRow(
    providerName: String,
    serviceIcon: ImageVector,
    tagLine: String,
    isConnected: Boolean,
    onConnectToggle: (Boolean) -> Unit,
    testPref: String
) {
    Card(
        border = BorderStroke(
            1.dp,
            if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    serviceIcon,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = providerName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = tagLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Switch(
                checked = isConnected,
                onCheckedChange = onConnectToggle,
                modifier = Modifier.testTag(testPref)
            )
        }
    }
}


// ------ SHARE UTILS ------
fun sharePdfFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            authority,
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Compiled PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing output: " + e.localizedMessage, Toast.LENGTH_LONG).show()
    }
}
