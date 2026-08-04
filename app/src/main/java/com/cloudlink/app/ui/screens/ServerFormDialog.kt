package com.cloudlink.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cloudlink.app.data.model.AuthType
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.model.ServerFolder
import com.cloudlink.app.ui.security.SecureWindowEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServerFormDialog(
    server: Server?,
    onDismiss: () -> Unit,
    onSave: (Server, String) -> Unit
) {
    SecureWindowEffect()
    var name by rememberSaveable(server?.id) { mutableStateOf(server?.name.orEmpty()) }
    var host by rememberSaveable(server?.id) { mutableStateOf(server?.host.orEmpty()) }
    var port by rememberSaveable(server?.id) { mutableStateOf(server?.port?.toString() ?: "22") }
    var username by rememberSaveable(server?.id) { mutableStateOf(server?.username.orEmpty()) }
    var authType by remember(server?.id) { mutableStateOf(server?.authType ?: AuthType.PASSWORD) }
    var credential by rememberSaveable(server?.id) { mutableStateOf("") }
    var folder by remember(server?.id) { mutableStateOf(server?.folder ?: ServerFolder.HOME_LAB) }
    var tags by rememberSaveable(server?.id) { mutableStateOf(server?.tags.orEmpty()) }
    var notes by rememberSaveable(server?.id) { mutableStateOf(server?.notes.orEmpty()) }
    var showCredential by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable(server?.id) {
        mutableStateOf(server?.tags?.isNotBlank() == true || server?.notes?.isNotBlank() == true)
    }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }

    val validPort = port.toIntOrNull()?.let { it in 1..65535 } == true
    val validHost = isValidServerHost(host)
    val credentialRequired = server == null || server.authType != authType
    val canSave = name.isNotBlank() && validHost && username.isNotBlank() && validPort &&
        (!credentialRequired || credential.isNotBlank())

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            val compact = maxWidth < 600.dp || maxHeight < 720.dp
            val panelModifier = if (compact) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .widthIn(max = 640.dp)
            }
            Surface(
                modifier = panelModifier,
                shape = if (compact) RectangleShape else MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            if (server == null) "Add server" else "Edit server",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Connection details stay on this device. Credentials are stored separately in encrypted storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Display name") },
                                isError = saveAttempted && name.isBlank(),
                                supportingText = if (saveAttempted && name.isBlank()) {
                                    { Text("Enter a name for this server.") }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it.trim() },
                                label = { Text("Host or IP address") },
                                placeholder = { Text("server.local, 192.0.2.10, or 2001:db8::10") },
                                isError = (saveAttempted || host.isNotEmpty()) && !validHost,
                                supportingText = if ((saveAttempted || host.isNotEmpty()) && !validHost) {
                                    { Text("Enter a DNS name, IPv4 address, or IPv6 literal without spaces.") }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                if (maxWidth < 420.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        PortField(port, { port = it }, validPort, saveAttempted, Modifier.fillMaxWidth())
                                        UsernameField(username, { username = it }, saveAttempted, Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        PortField(port, { port = it }, validPort, saveAttempted, Modifier.weight(0.38f))
                                        UsernameField(username, { username = it }, saveAttempted, Modifier.weight(0.62f))
                                    }
                                }
                            }
                        }
                        item {
                            Text("Authentication", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = authType == AuthType.PASSWORD,
                                    onClick = { authType = AuthType.PASSWORD },
                                    label = { Text("Password") }
                                )
                                FilterChip(
                                    selected = authType == AuthType.KEY,
                                    onClick = { authType = AuthType.KEY },
                                    label = { Text("Private key") }
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = credential,
                                onValueChange = { credential = it },
                                label = { Text(if (authType == AuthType.PASSWORD) "Password" else "Private key") },
                                isError = saveAttempted && credentialRequired && credential.isBlank(),
                                supportingText = {
                                    Text(
                                        when {
                                            credentialRequired && server != null -> "Required because the authentication method changed."
                                            credentialRequired -> "Required for the first connection."
                                            else -> "Leave blank to keep the saved credential."
                                        }
                                    )
                                },
                                singleLine = authType == AuthType.PASSWORD,
                                minLines = if (authType == AuthType.KEY) 4 else 1,
                                maxLines = if (authType == AuthType.KEY) 8 else 1,
                                visualTransformation = if (authType == AuthType.PASSWORD && !showCredential) {
                                    PasswordVisualTransformation()
                                } else VisualTransformation.None,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (authType == AuthType.PASSWORD) KeyboardType.Password else KeyboardType.Text
                                ),
                                trailingIcon = if (authType == AuthType.PASSWORD) {
                                    {
                                        IconButton(onClick = { showCredential = !showCredential }) {
                                            Icon(
                                                if (showCredential) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (showCredential) "Hide password" else "Show password"
                                            )
                                        }
                                    }
                                } else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Folder", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ServerFolder.entries.filterNot { it == ServerFolder.ALL }.forEach { option ->
                                    FilterChip(
                                        selected = folder == option,
                                        onClick = { folder = option },
                                        label = { Text(option.displayName) }
                                    )
                                }
                            }
                        }
                        item {
                            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                                Icon(
                                    if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null
                                )
                                Text(if (showAdvanced) "Hide organization fields" else "Show tags and notes")
                            }
                        }
                        if (showAdvanced) {
                            item {
                                OutlinedTextField(
                                    value = tags,
                                    onValueChange = { tags = it },
                                    label = { Text("Tags") },
                                    supportingText = { Text("Separate tags with commas.") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Notes") },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Button(
                            onClick = {
                                saveAttempted = true
                                if (canSave) {
                                    onSave(
                                        Server(
                                            id = server?.id ?: 0,
                                            name = name.trim(),
                                            host = host.trim(),
                                            port = port.toInt(),
                                            username = username.trim(),
                                            authType = authType,
                                            folder = folder,
                                            favorite = server?.favorite ?: false,
                                            notes = notes.trim(),
                                            tags = tags.trim()
                                        ),
                                        credential
                                    )
                                }
                            }
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortField(
    value: String,
    onValueChange: (String) -> Unit,
    valid: Boolean,
    saveAttempted: Boolean,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        label = { Text("Port") },
        isError = (saveAttempted || value.isNotEmpty()) && !valid,
        supportingText = if ((saveAttempted || value.isNotEmpty()) && !valid) {
            { Text("Use 1–65535.") }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    saveAttempted: Boolean,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Username") },
        isError = saveAttempted && value.isBlank(),
        supportingText = if (saveAttempted && value.isBlank()) {
            { Text("Enter the SSH user.") }
        } else null,
        singleLine = true,
        modifier = modifier
    )
}

internal fun isValidServerHost(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > 253 || trimmed.any(Char::isWhitespace)) return false
    if (':' in trimmed) {
        return runCatching { java.net.InetAddress.getByName(trimmed).address.size == 16 }.getOrDefault(false)
    }
    return Regex("^[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?$").matches(trimmed) && ".." !in trimmed
}
