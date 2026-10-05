package com.cloudlink.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cloudlink.app.BuildConfig
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel
import com.cloudlink.app.ui.components.CloudSectionHeader
import com.cloudlink.app.ui.theme.ThemeSelectorBottomSheet
import com.cloudlink.app.ui.viewmodel.SettingsViewModel
import com.cloudlink.app.data.network.SshConnectionManager
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val secureScreen by viewModel.secureScreen.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val knownHosts by viewModel.knownHosts.collectAsStateWithLifecycle()

    var showThemeSelector by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var showKnownHosts by remember { mutableStateOf(false) }

    if (showThemeSelector) {
        ThemeSelectorBottomSheet(
            currentTheme = currentTheme,
            onThemeSelected = {
                viewModel.setTheme(it)
                showThemeSelector = false
            },
            onDismiss = { showThemeSelector = false }
        )
    }

    if (showLicenses) {
        InformationDialog(
            title = "Open-source licenses",
            body = "AndroidX and Jetpack Compose — Apache License 2.0\n\n" +
                "Kotlin and Kotlin Coroutines — Apache License 2.0\n\n" +
                "Hilt and Dagger — Apache License 2.0\n\n" +
                "JSch (mwiede fork) — BSD-style license",
            onDismiss = { showLicenses = false }
        )
    }

    if (showAbout) {
        InformationDialog(
            title = "About CloudLink",
            body = "CloudLink keeps saved infrastructure, secure SSH sessions, file access, and network utilities together on your device.",
            onDismiss = { showAbout = false }
        )
    }

    if (showKnownHosts) {
        KnownHostsDialog(
            entries = knownHosts,
            onRemove = viewModel::removeKnownHost,
            onDismiss = { showKnownHosts = false }
        )
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear connection history?") },
            text = { Text("This removes all recorded connection events. Saved servers and credentials are not affected.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearConnectionHistory()
                    confirmClearHistory = false
                    scope.launch { snackbarHostState.showSnackbar("Connection history cleared") }
                }) { Text("Clear history") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset preferences?") },
            text = { Text("Theme returns to Modern Dark and screen protection is turned on. Servers and credentials remain untouched.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetPreferences()
                    confirmReset = false
                    scope.launch { snackbarHostState.showSnackbar("Preferences reset") }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SettingsGroup(
                            title = "Privacy",
                            supportingText = "Protection applied across CloudLink"
                        ) {
                            SettingsToggleRow(
                                icon = Icons.Rounded.Security,
                                title = "Screen protection",
                                supportingText = "Block screenshots and screen recording while CloudLink is visible.",
                                checked = secureScreen,
                                onCheckedChange = viewModel::setSecureScreen
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Rounded.Key,
                                title = "Known SSH hosts",
                                supportingText = if (knownHosts.isEmpty()) {
                                    "No host keys trusted on this device."
                                } else {
                                    "${knownHosts.size} trusted ${if (knownHosts.size == 1) "key" else "keys"} on this device."
                                },
                                onClick = {
                                    viewModel.refreshKnownHosts()
                                    showKnownHosts = true
                                }
                            )
                        }
                    }

                    item {
                        SettingsGroup(
                            title = "Appearance",
                            supportingText = "Choose how the application looks"
                        ) {
                            SettingsActionRow(
                                icon = Icons.Rounded.Palette,
                                title = "Theme",
                                supportingText = currentTheme.displayName,
                                onClick = { showThemeSelector = true }
                            )
                        }
                    }

                    item {
                        SettingsGroup(
                            title = "Data",
                            supportingText = "Connection events stored on this device"
                        ) {
                            SettingsActionRow(
                                icon = Icons.Rounded.Share,
                                title = "Share connection history",
                                supportingText = "Export recorded connection events as plain text.",
                                onClick = {
                                    scope.launch {
                                        val export = viewModel.createConnectionHistoryExport()
                                        if (export == null) {
                                            snackbarHostState.showSnackbar("There is no connection history to export")
                                        } else {
                                            runCatching {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "CloudLink connection history")
                                                    putExtra(Intent.EXTRA_TEXT, export)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share connection history"))
                                            }.onFailure {
                                                snackbarHostState.showSnackbar("No compatible sharing application found")
                                            }
                                        }
                                    }
                                }
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Rounded.DeleteSweep,
                                title = "Clear connection history",
                                supportingText = "Saved servers and credentials are preserved.",
                                destructive = true,
                                onClick = { confirmClearHistory = true }
                            )
                        }
                    }

                    item {
                        SettingsGroup(
                            title = "Application",
                            supportingText = "Build information and notices"
                        ) {
                            SettingsInfoRow(
                                icon = Icons.Rounded.Info,
                                title = "Version",
                                value = BuildConfig.VERSION_NAME
                            )
                            SettingsDivider()
                            SettingsInfoRow(
                                icon = Icons.Rounded.SettingsBackupRestore,
                                title = "Build",
                                value = BuildConfig.VERSION_CODE.toString()
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Rounded.Description,
                                title = "Open-source licenses",
                                supportingText = "Libraries and license notices",
                                onClick = { showLicenses = true }
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Rounded.Info,
                                title = "About CloudLink",
                                supportingText = "What this application is built for",
                                onClick = { showAbout = true }
                            )
                        }
                    }

                    item {
                        SettingsGroup(
                            title = "Defaults",
                            supportingText = "Restore application preferences"
                        ) {
                            SettingsActionRow(
                                icon = Icons.Rounded.RestartAlt,
                                title = "Reset preferences",
                                supportingText = "Restore the default theme and privacy protection.",
                                onClick = { confirmReset = true }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    supportingText: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CloudSectionHeader(title = title, supportingText = supportingText)
        CloudPanel(modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = contentColor)
            Spacer(Modifier.height(3.dp))
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.78f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

@Composable
private fun InformationDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun KnownHostsDialog(
    entries: List<SshConnectionManager.KnownHostEntry>,
    onRemove: (SshConnectionManager.KnownHostEntry) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Known SSH hosts") },
        text = {
            if (entries.isEmpty()) {
                Text("No host keys are trusted yet. A verified first connection will appear here.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        CloudPanel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(entry.host, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    entry.algorithm,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    entry.fingerprint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { onRemove(entry) },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Remove trust") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
