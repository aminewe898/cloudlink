package com.cloudlink.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudlink.app.ui.components.CloudEmptyState
import com.cloudlink.app.ui.viewmodel.SftpViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    serverId: Int = -1,
    filePath: String = "",
    viewModel: SftpViewModel? = null,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val remoteError = viewModel?.errorMessage?.collectAsStateWithLifecycle()?.value
    var content by remember(filePath) { mutableStateOf("") }
    var savedContent by remember(filePath) { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var reloadSignal by remember { mutableIntStateOf(0) }
    var fontSize by rememberSaveable { mutableIntStateOf(14) }
    val fileName = filePath.substringAfterLast('/').ifBlank { "Editor" }
    val dirty = content != savedContent

    fun requestBack() {
        if (dirty) confirmDiscard = true else onNavigateBack()
    }

    fun save() {
        if (viewModel == null || isLoading || isSaving || !dirty) return
        scope.launch {
            isSaving = true
            saveMessage = "Saving…"
            val success = viewModel.saveFileContent(filePath, content)
            if (success) {
                savedContent = content
                saveMessage = "Saved"
            } else {
                saveMessage = remoteError ?: "Save failed"
            }
            isSaving = false
        }
    }

    LaunchedEffect(serverId, filePath, reloadSignal) {
        if (serverId < 0 || filePath.isBlank() || viewModel == null) {
            loadFailed = true
            return@LaunchedEffect
        }
        isLoading = true
        loadFailed = false
        saveMessage = null
        viewModel.initServer(serverId)
        val loaded = viewModel.readFileContent(filePath)
        if (loaded == null) {
            loadFailed = true
        } else {
            content = loaded
            savedContent = loaded
        }
        isLoading = false
    }

    BackHandler(enabled = dirty) { confirmDiscard = true }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = when {
                                isLoading -> "Loading…"
                                isSaving -> "Saving…"
                                dirty -> "Unsaved changes"
                                saveMessage != null -> saveMessage.orEmpty()
                                else -> filePath
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (dirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fontSize = (fontSize - 1).coerceAtLeast(10) }, enabled = fontSize > 10) {
                        Icon(Icons.Rounded.TextDecrease, contentDescription = "Smaller text")
                    }
                    IconButton(onClick = { fontSize = (fontSize + 1).coerceAtMost(24) }, enabled = fontSize < 24) {
                        Icon(Icons.Rounded.TextIncrease, contentDescription = "Larger text")
                    }
                    IconButton(onClick = ::save, enabled = dirty && !isLoading && !isSaving) {
                        if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Save, contentDescription = "Save file")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            if (!isLoading && !loadFailed) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${content.lineSequence().count()} lines  ·  ${content.length} characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${fontSize}sp  ·  UTF-8",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                loadFailed -> CloudEmptyState(
                    icon = Icons.Rounded.Refresh,
                    title = "Could not open this file",
                    supportingText = remoteError ?: "The file may be unavailable, too large, or not valid UTF-8 text.",
                    actionLabel = "Try again",
                    onAction = { reloadSignal++ },
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()
                    BasicTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            saveMessage = null
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "Remote text file editor" }
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .padding(16.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize + 6).sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { editor ->
                            Box(Modifier.fillMaxSize()) {
                                if (content.isEmpty()) {
                                    Text(
                                        "Empty file — start typing",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSize.sp
                                    )
                                }
                                editor()
                            }
                        }
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("Changes to $fileName have not been saved to the server.") },
            confirmButton = {
                Button(onClick = {
                    confirmDiscard = false
                    onNavigateBack()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
        )
    }
}
