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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cloudlink.app.ui.components.CloudEmptyState
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel
import com.cloudlink.app.ui.components.CloudSectionHeader
import com.cloudlink.app.ui.components.StatusPill
import com.cloudlink.app.ui.viewmodel.SessionItem
import com.cloudlink.app.ui.viewmodel.SessionsViewModel
import java.text.DateFormat
import java.util.Date
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenSession: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionCount = uiState.active.size

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sessions", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (sessionCount == 1) "1 connection active" else "$sessionCount connections active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToServers) {
                        Icon(Icons.Rounded.Dns, contentDescription = "Browse servers")
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
            if (uiState.active.isEmpty() && uiState.recent.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CloudEmptyState(
                        icon = Icons.Rounded.Terminal,
                        title = "No sessions yet",
                        supportingText = "Open a saved server to start a secure terminal. Your recent connections will appear here.",
                        actionLabel = "Browse servers",
                        onAction = onNavigateToServers
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.active.isNotEmpty()) {
                        item {
                            CloudSectionHeader(
                                title = "Active now",
                                supportingText = "Secure SSH transports available on this device"
                            )
                        }
                        items(uiState.active, key = { "active-${it.server.id}" }) { session ->
                            SessionCard(
                                item = session,
                                active = true,
                                onOpen = { onOpenSession(session.server.id) },
                                onDisconnect = { viewModel.disconnect(session.server.id) }
                            )
                        }
                    }

                    if (uiState.recent.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(if (uiState.active.isEmpty()) 0.dp else 8.dp))
                            CloudSectionHeader(
                                title = "Recent",
                                supportingText = "Your latest connection activity"
                            )
                        }
                        items(uiState.recent, key = { "recent-${it.server.id}" }) { session ->
                            SessionCard(
                                item = session,
                                active = false,
                                onOpen = { onOpenSession(session.server.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    item: SessionItem,
    active: Boolean,
    onOpen: () -> Unit,
    onDisconnect: (() -> Unit)? = null
) {
    val timeLabel = remember(item.lastActivityAt) {
        if (item.lastActivityAt <= 0L) "Started now" else {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.lastActivityAt))
        }
    }
    val statusColor = if (active) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant

    CloudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (active) Icons.Rounded.Terminal else Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.server.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.server.username}@${item.server.host}:${item.server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    label = if (active) "Active" else "Recent",
                    color = statusColor,
                    leadingContent = {
                        Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                    }
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (active) "Connected securely" else timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (active && onDisconnect != null) {
                    TextButton(
                        onClick = onDisconnect,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                    Spacer(Modifier.width(4.dp))
                }
                FilledTonalButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (active) "Resume" else "Reconnect")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
