package com.cloudlink.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cloudlink.app.data.model.RemoteFile
import com.cloudlink.app.data.network.SftpTransferService
import com.cloudlink.app.ui.components.CloudEmptyState
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel
import com.cloudlink.app.ui.viewmodel.SftpViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    viewModel: SftpViewModel,
    serverId: Int,
    onNavigateBack: () -> Unit,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFiles by remember { mutableStateOf<Set<RemoteFile>>(emptySet()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var createDirectory by remember { mutableStateOf(true) }
    var entryName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<RemoteFile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var fileToDownload by remember { mutableStateOf<RemoteFile?>(null) }
    var detailsFile by remember { mutableStateOf<RemoteFile?>(null) }
    var pendingTransfer by remember { mutableStateOf<PendingTransfer?>(null) }
    val isSelectionMode = selectedFiles.isNotEmpty()

    fun launchTransfer(transfer: PendingTransfer) {
        val intent = Intent(context, SftpTransferService::class.java).apply {
            this.action = transfer.action
            putExtra(SftpTransferService.EXTRA_SERVER_ID, serverId)
            putExtra(SftpTransferService.EXTRA_REMOTE_PATH, transfer.remotePath)
            putExtra(SftpTransferService.EXTRA_LOCAL_URI, transfer.uri.toString())
        }
        ContextCompat.startForegroundService(context, intent)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val transfer = pendingTransfer
        pendingTransfer = null
        if (granted && transfer != null) {
            launchTransfer(transfer)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Notification permission is required to show transfer progress.")
            }
        }
    }

    fun startTransfer(action: String, remotePath: String, uri: android.net.Uri) {
        val transfer = PendingTransfer(action, remotePath, uri)
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingTransfer = transfer
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchTransfer(transfer)
        }
    }

    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val file = fileToDownload
        if (uri != null && file != null) {
            startTransfer(SftpTransferService.ACTION_DOWNLOAD, remotePath(currentPath, file.name), uri)
            scope.launch { snackbarHostState.showSnackbar("Download started") }
        }
        selectedFiles = emptySet()
        fileToDownload = null
    }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            startTransfer(SftpTransferService.ACTION_UPLOAD, currentPath, uri)
            scope.launch { snackbarHostState.showSnackbar("Upload started") }
        }
    }

    LaunchedEffect(serverId) {
        if (serverId >= 0) viewModel.initServer(serverId)
    }
    LaunchedEffect(currentPath) { selectedFiles = emptySet() }

    BackHandler(enabled = isSelectionMode || currentPath != "/") {
        if (isSelectionMode) selectedFiles = emptySet() else viewModel.navigateUp()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedFiles.size} selected", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { selectedFiles = emptySet() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedFiles = files.toSet() }) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                        }
                        if (selectedFiles.size == 1) {
                            val selected = selectedFiles.first()
                            IconButton(onClick = {
                                renameTarget = selected
                                entryName = selected.name
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                            }
                            if (!selected.isDirectory) {
                                IconButton(onClick = {
                                    fileToDownload = selected
                                    downloadLauncher.launch(selected.name)
                                }) {
                                    Icon(Icons.Rounded.Download, contentDescription = "Download")
                                }
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete selected files")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text("Files", style = MaterialTheme.typography.titleLarge)
                            Text(
                                currentPath,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (currentPath == "/") onNavigateBack() else viewModel.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = if (currentPath == "/") "Back" else "Parent folder")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadDirectory(currentPath) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh folder")
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = "File actions")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Upload file") },
                                    leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                                    onClick = { menuExpanded = false; uploadLauncher.launch("*/*") }
                                )
                                DropdownMenuItem(
                                    text = { Text("New folder") },
                                    leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        createDirectory = true
                                        entryName = ""
                                        showEntryDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New text file") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        createDirectory = false
                                        entryName = ""
                                        showEntryDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }
        }
    ) { innerPadding ->
        CloudLinkBackdrop(Modifier.fillMaxSize().padding(innerPadding)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when {
                    serverId < 0 -> CloudEmptyState(
                        icon = Icons.Rounded.Folder,
                        title = "No server selected",
                        supportingText = "Return to Servers and choose a connection first.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    errorMessage != null && files.isEmpty() -> CloudEmptyState(
                        icon = Icons.Rounded.Folder,
                        title = "Could not open this folder",
                        supportingText = errorMessage.orEmpty(),
                        actionLabel = "Try again",
                        onAction = { viewModel.loadDirectory(currentPath) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    isLoading && files.isEmpty() -> LinearProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).widthIn(max = 240.dp).fillMaxWidth()
                    )
                    files.isEmpty() -> CloudEmptyState(
                        icon = Icons.Rounded.Folder,
                        title = "This folder is empty",
                        supportingText = "Upload a file or create a new entry here.",
                        actionLabel = "Upload file",
                        onAction = { uploadLauncher.launch("*/*") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        modifier = Modifier.widthIn(max = 820.dp).fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        items(files, key = { it.name }) { file ->
                            RemoteFileRow(
                                file = file,
                                selected = file in selectedFiles,
                                selectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedFiles = if (file in selectedFiles) selectedFiles - file else selectedFiles + file
                                    } else if (file.isDirectory) {
                                        viewModel.loadDirectory(remotePath(currentPath, file.name))
                                    } else {
                                        onOpenFile(remotePath(currentPath, file.name))
                                    }
                                },
                                onLongClick = { selectedFiles = selectedFiles + file },
                                onDetails = { detailsFile = file }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEntryDialog) {
        AlertDialog(
            onDismissRequest = { showEntryDialog = false },
            icon = { Icon(if (createDirectory) Icons.Rounded.CreateNewFolder else Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null) },
            title = { Text(if (createDirectory) "New folder" else "New text file") },
            text = {
                OutlinedTextField(
                    value = entryName,
                    onValueChange = { entryName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = entryName.isNotBlank(),
                    onClick = {
                        if (createDirectory) viewModel.createFolder(entryName) else viewModel.createFile(entryName)
                        showEntryDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showEntryDialog = false }) { Text("Cancel") } }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename ${target.name}") },
            text = {
                OutlinedTextField(
                    value = entryName,
                    onValueChange = { entryName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = entryName.isNotBlank() && entryName != target.name,
                    onClick = {
                        viewModel.renameFile(target.name, entryName)
                        selectedFiles = emptySet()
                        renameTarget = null
                    }
                ) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedFiles.size} ${if (selectedFiles.size == 1) "item" else "items"}?") },
            text = { Text("Remote deletion cannot be undone. Non-empty folders may need to be emptied first.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFiles(selectedFiles)
                        selectedFiles = emptySet()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    detailsFile?.let { file ->
        AlertDialog(
            onDismissRequest = { detailsFile = null },
            title = { Text(file.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetadataDetail("Type", if (file.isDirectory) "Folder" else "File")
                    MetadataDetail("Permissions", file.permissions.ifBlank { "Unavailable" })
                    MetadataDetail("Size", file.size.ifBlank { "Unavailable" })
                    MetadataDetail("Modified", file.modifiedDate.ifBlank { "Unavailable" })
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsFile = null }) { Text("Done") }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RemoteFileRow(
    file: RemoteFile,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDetails: () -> Unit
) {
    CloudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = buildString {
                    append(if (file.isDirectory) "Folder" else if (file.isSymbolicLink) "Symbolic link" else "File")
                    append(", ${file.name}")
                    if (file.size.isNotBlank()) append(", ${file.size}")
                    if (file.permissions.isNotBlank()) append(", ${file.permissions}")
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else if (file.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                contentDescription = null,
                tint = if (selected || file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    listOf(file.permissions, file.size).filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.modifiedDate.isNotBlank()) {
                    Text(
                        file.modifiedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!selectionMode) {
                IconButton(onClick = onDetails) {
                    Icon(Icons.Rounded.Info, contentDescription = "Details for ${file.name}")
                }
            }
        }
    }
}

@Composable
private fun MetadataDetail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun remotePath(parent: String, name: String): String =
    if (parent == "/") "/$name" else "${parent.trimEnd('/')}/$name"

private data class PendingTransfer(
    val action: String,
    val remotePath: String,
    val uri: android.net.Uri
)
