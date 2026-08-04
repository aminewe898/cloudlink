package com.cloudlink.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudlink.app.ui.security.SecureWindowEffect
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel
import com.cloudlink.app.ui.components.CloudSectionHeader
import com.cloudlink.app.ui.components.StatusPill
import com.cloudlink.app.ui.theme.CloudLinkThemeValues
import com.cloudlink.app.ui.viewmodel.ToolsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class ToolboxTool(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector
) {
    PING(
        title = "Ping",
        subtitle = "Check whether a host responds",
        description = "Send four network probes and review latency and packet loss.",
        icon = Icons.Rounded.NetworkPing
    ),
    WAKE_ON_LAN(
        title = "Wake on LAN",
        subtitle = "Start a device remotely",
        description = "Send a UDP magic packet to a device on your network.",
        icon = Icons.Rounded.PowerSettingsNew
    ),
    SSH_KEY(
        title = "SSH key pair",
        subtitle = "Generate RSA 4096 credentials",
        description = "Create a local public and private key pair for SSH authentication.",
        icon = Icons.Rounded.Key
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTool by remember { mutableStateOf<ToolboxTool?>(null) }
    var primaryInput by remember { mutableStateOf("") }
    var secondaryInput by remember { mutableStateOf("") }
    val output by viewModel.toolOutput.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val lastRunSucceeded by viewModel.lastRunSucceeded.collectAsStateWithLifecycle()
    val generatedSshKey by viewModel.generatedSshKey.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var confirmPrivateKeyReveal by remember { mutableStateOf(false) }
    var revealedPrivateKey by remember { mutableStateOf<String?>(null) }

    if (selectedTool == ToolboxTool.SSH_KEY) SecureWindowEffect()

    fun openTool(tool: ToolboxTool) {
        selectedTool = tool
        primaryInput = ""
        secondaryInput = ""
        viewModel.clearOutput()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedTool?.title ?: "Toolbox", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = selectedTool?.subtitle ?: "Network diagnostics and secure access",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (selectedTool != null) {
                        IconButton(onClick = {
                            viewModel.cancelExecution()
                            selectedTool = null
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to toolbox")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        CloudLinkBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val tool = selectedTool
            if (tool == null) {
                ToolboxHome(onOpenTool = ::openTool)
            } else {
                ToolWorkspace(
                    tool = tool,
                    primaryInput = primaryInput,
                    onPrimaryInputChange = { primaryInput = it },
                    secondaryInput = secondaryInput,
                    onSecondaryInputChange = { secondaryInput = it },
                    output = output,
                    isExecuting = isExecuting,
                    lastRunSucceeded = lastRunSucceeded,
                    onRun = {
                        when (tool) {
                            ToolboxTool.PING -> viewModel.runPing(primaryInput)
                            ToolboxTool.WAKE_ON_LAN -> viewModel.runWakeOnLan(primaryInput, secondaryInput)
                            ToolboxTool.SSH_KEY -> viewModel.generateSshKey()
                        }
                    },
                    onCancel = viewModel::cancelExecution,
                    onClearOutput = viewModel::clearOutput,
                    hasPrivateKey = generatedSshKey != null,
                    onRevealPrivateKey = { confirmPrivateKeyReveal = true }
                )
            }
        }
    }

    if (confirmPrivateKeyReveal) {
        AlertDialog(
            onDismissRequest = { confirmPrivateKeyReveal = false },
            title = { Text("Reveal private key?") },
            text = {
                Text("Anyone who obtains this private key can authenticate as you. Keep screen protection enabled and store it only in a trusted location.")
            },
            confirmButton = {
                Button(onClick = {
                    revealedPrivateKey = generatedSshKey?.privateKey
                    confirmPrivateKeyReveal = false
                }) { Text("Reveal") }
            },
            dismissButton = {
                TextButton(onClick = { confirmPrivateKeyReveal = false }) { Text("Cancel") }
            }
        )
    }

    revealedPrivateKey?.let { privateKey ->
        AlertDialog(
            onDismissRequest = { revealedPrivateKey = null },
            title = { Text("Private key") },
            text = {
                SelectionContainer {
                    Text(
                        privateKey,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(privateKey))
                    scope.launch {
                        delay(60_000)
                        if (clipboard.getText()?.text == privateKey) {
                            clipboard.setText(AnnotatedString(""))
                        }
                    }
                }) { Text("Copy for 60 seconds") }
            },
            dismissButton = {
                TextButton(onClick = { revealedPrivateKey = null }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun ToolboxHome(onOpenTool: (ToolboxTool) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        CloudSectionHeader(
            title = "Utilities",
            supportingText = "Small, focused tools that run directly on this device.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(ToolboxTool.entries) { tool ->
                ToolboxEntry(tool = tool, onClick = { onOpenTool(tool) })
            }
        }
    }
}

@Composable
private fun ToolboxEntry(tool: ToolboxTool, onClick: () -> Unit) {
    CloudPanel(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tool.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tool.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ToolWorkspace(
    tool: ToolboxTool,
    primaryInput: String,
    onPrimaryInputChange: (String) -> Unit,
    secondaryInput: String,
    onSecondaryInputChange: (String) -> Unit,
    output: String,
    isExecuting: Boolean,
    lastRunSucceeded: Boolean?,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onClearOutput: () -> Unit,
    hasPrivateKey: Boolean,
    onRevealPrivateKey: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val semanticColors = CloudLinkThemeValues.semanticColors
    val canRun = when (tool) {
        ToolboxTool.PING -> primaryInput.isNotBlank()
        ToolboxTool.WAKE_ON_LAN -> primaryInput.isNotBlank()
        ToolboxTool.SSH_KEY -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CloudPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (tool) {
                    ToolboxTool.PING -> {
                        OutlinedTextField(
                            value = primaryInput,
                            onValueChange = onPrimaryInputChange,
                            label = { Text("Host or IP address") },
                            placeholder = { Text("server.local or 192.168.1.10") },
                            enabled = !isExecuting,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ToolboxTool.WAKE_ON_LAN -> {
                        OutlinedTextField(
                            value = primaryInput,
                            onValueChange = onPrimaryInputChange,
                            label = { Text("MAC address") },
                            placeholder = { Text("00:11:22:33:44:55") },
                            enabled = !isExecuting,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = secondaryInput,
                            onValueChange = onSecondaryInputChange,
                            label = { Text("Broadcast address") },
                            placeholder = { Text("255.255.255.255") },
                            supportingText = { Text("Optional · UDP port 9") },
                            enabled = !isExecuting,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ToolboxTool.SSH_KEY -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(semanticColors.warningContainer, MaterialTheme.shapes.medium)
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                contentDescription = null,
                                tint = semanticColors.onWarningContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "The private key is shown once in the output. Copy it to secure storage before leaving this tool.",
                                style = MaterialTheme.typography.bodySmall,
                                color = semanticColors.onWarningContainer
                            )
                        }
                    }
                }

                if (isExecuting) {
                    FilledTonalButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel")
                    }
                } else {
                    Button(onClick = onRun, enabled = canRun, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (tool) {
                                ToolboxTool.PING -> "Run ping"
                                ToolboxTool.WAKE_ON_LAN -> "Send magic packet"
                                ToolboxTool.SSH_KEY -> "Generate key pair"
                            }
                        )
                    }
                }
            }
        }

        CloudPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Output", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (lastRunSucceeded != null) {
                        StatusPill(
                            label = if (lastRunSucceeded == true) "Complete" else "Needs attention",
                            color = if (lastRunSucceeded == true) semanticColors.success else MaterialTheme.colorScheme.error
                        )
                    }
                    if (output.isNotBlank()) {
                        if (tool == ToolboxTool.SSH_KEY && hasPrivateKey) {
                            FilledTonalButton(onClick = onRevealPrivateKey) {
                                Icon(Icons.Rounded.Key, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Reveal private key")
                            }
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(output)) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy output")
                        }
                        IconButton(onClick = onClearOutput, enabled = !isExecuting) {
                            Icon(Icons.Rounded.ClearAll, contentDescription = "Clear output")
                        }
                    }
                }
                if (isExecuting) LinearProgressIndicator(Modifier.fillMaxWidth())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = output.ifBlank { "Run the tool to see its output here." },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (output.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}
