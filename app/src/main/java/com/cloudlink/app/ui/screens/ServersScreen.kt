package com.cloudlink.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.model.ServerFolder
import com.cloudlink.app.ui.components.CloudEmptyState
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel
import com.cloudlink.app.ui.components.StatusPill
import com.cloudlink.app.ui.viewmodel.ServerListViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    serverListViewModel: ServerListViewModel,
    onNavigateToTerminal: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by serverListViewModel.servers.collectAsStateWithLifecycle()
    val currentFolder by serverListViewModel.currentFolder.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<Server?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(serverListViewModel) {
        serverListViewModel.operationErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val filteredServers = remember(servers, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) servers else servers.filter { server ->
            server.name.contains(query, ignoreCase = true) ||
                server.host.contains(query, ignoreCase = true) ||
                server.username.contains(query, ignoreCase = true) ||
                server.tags.contains(query, ignoreCase = true)
        }
    }
    val hasAnyServers = servers.isNotEmpty() || currentFolder != ServerFolder.ALL

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Servers", style = MaterialTheme.typography.titleLarge)
                        if (hasAnyServers) {
                            Text(
                                text = "${servers.size} saved in ${currentFolder.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (hasAnyServers) {
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search servers")
                        }
                        IconButton(onClick = {
                            serverToEdit = null
                            showServerDialog = true
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add server")
                        }
                    }
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
            if (!hasAnyServers) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CloudEmptyState(
                        icon = Icons.Rounded.Dns,
                        title = "Add your first server",
                        supportingText = "Save an SSH connection once, then open its terminal or files whenever you need it.",
                        actionLabel = "Add server",
                        onAction = {
                            serverToEdit = null
                            showServerDialog = true
                        }
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (searchVisible || searchQuery.isNotEmpty()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Name, host, user, or tag") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    searchVisible = false
                                }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Close search")
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ServerFolder.entries) { folder ->
                            FilterChip(
                                selected = currentFolder == folder,
                                onClick = { serverListViewModel.setFolder(folder) },
                                label = { Text(folder.displayName) },
                                leadingIcon = if (currentFolder == folder) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    if (filteredServers.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CloudEmptyState(
                                icon = Icons.Rounded.SearchOff,
                                title = if (searchQuery.isBlank()) "No servers in this folder" else "No matching servers",
                                supportingText = if (searchQuery.isBlank()) {
                                    "Choose another folder or add a server here."
                                } else {
                                    "Try a different name, host, user, or tag."
                                },
                                actionLabel = if (searchQuery.isBlank()) "Add server" else "Clear search",
                                onAction = {
                                    if (searchQuery.isBlank()) {
                                        serverToEdit = null
                                        showServerDialog = true
                                    } else {
                                        searchQuery = ""
                                    }
                                }
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 300.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredServers, key = { it.id }) { server ->
                                ServerConnectionCard(
                                    server = server,
                                    onConnect = { onNavigateToTerminal(server.id) },
                                    onEdit = {
                                        serverToEdit = server
                                        showServerDialog = true
                                    },
                                    onDelete = { serverListViewModel.deleteServer(server) },
                                    onToggleFavorite = { serverListViewModel.toggleFavorite(server) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showServerDialog) {
        ServerFormDialog(
            server = serverToEdit,
            onDismiss = { showServerDialog = false },
            onSave = { updated, credential ->
                if (serverToEdit == null) {
                    serverListViewModel.addServer(updated, credential)
                } else {
                    serverListViewModel.updateServer(updated, credential)
                }
                showServerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerConnectionCard(
    server: Server,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    CloudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onConnect, onLongClick = { menuExpanded = true })
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (server.folder == ServerFolder.RASPBERRY_PIS) Icons.Rounded.Bolt else Icons.Rounded.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (server.favorite) {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Text(
                        text = "${server.username}@${server.host}:${server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Server options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Open terminal") },
                            leadingIcon = { Icon(Icons.Rounded.Terminal, contentDescription = null) },
                            onClick = { menuExpanded = false; onConnect() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (server.favorite) "Remove favorite" else "Add favorite") },
                            leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) },
                            onClick = { menuExpanded = false; onToggleFavorite() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; confirmDelete = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(label = server.folder.displayName, color = MaterialTheme.colorScheme.secondary)
                val firstTag = server.tags.split(',').map { it.trim() }.firstOrNull { it.isNotEmpty() }
                firstTag?.let {
                    Spacer(Modifier.width(6.dp))
                    StatusPill(label = it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onConnect, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Connect")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${server.name}?") },
            text = { Text("This removes the saved server, its credentials, and connection history from this device.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
