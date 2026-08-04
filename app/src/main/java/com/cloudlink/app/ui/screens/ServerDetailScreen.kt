package com.cloudlink.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cloudlink.app.data.model.Resource
import com.cloudlink.app.ui.components.CloudEmptyState
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel
import com.cloudlink.app.ui.components.CloudSectionHeader
import com.cloudlink.app.ui.components.LineChart
import com.cloudlink.app.ui.components.StatusPill
import com.cloudlink.app.ui.theme.CloudLinkThemeValues
import com.cloudlink.app.ui.viewmodel.DashboardViewModel
import com.cloudlink.app.ui.viewmodel.ServerMonitorStatus
import com.cloudlink.app.ui.viewmodel.SystemInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServerDetailScreen(
    serverId: Int,
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTerminal: (Int) -> Unit,
    onNavigateToSftp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val systemInfo by viewModel.systemInfo.collectAsStateWithLifecycle()
    val cpuUsage by viewModel.cpuUsage.collectAsStateWithLifecycle()
    val ramUsage by viewModel.ramUsage.collectAsStateWithLifecycle()
    val cpuHistory by viewModel.cpuHistory.collectAsStateWithLifecycle()
    val ramHistory by viewModel.ramHistory.collectAsStateWithLifecycle()
    val uptime by viewModel.uptime.collectAsStateWithLifecycle()
    val loadAvg by viewModel.loadAvg.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val pingMs by viewModel.pingMs.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()
    val monitorStatus by viewModel.monitorStatus.collectAsStateWithLifecycle()
    val monitorError by viewModel.monitorError.collectAsStateWithLifecycle()

    LaunchedEffect(serverId) { viewModel.loadServer(serverId) }
    DisposableEffect(viewModel) {
        onDispose { viewModel.pauseMonitoring() }
    }

    val title = (serverInfo as? Resource.Success)?.data?.name ?: "Server"
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = monitorStatus.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (serverInfo is Resource.Success) {
                        IconButton(onClick = viewModel::retry) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh server details")
                        }
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
            when (val resource = serverInfo) {
                Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(Modifier.widthIn(max = 240.dp).fillMaxWidth())
                }
                is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CloudEmptyState(
                        icon = Icons.Rounded.Computer,
                        title = "Server unavailable",
                        supportingText = resource.message,
                        actionLabel = "Try again",
                        onAction = viewModel::retry
                    )
                }
                is Resource.Success -> {
                    val server = resource.data
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                CloudPanel(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(server.name, style = MaterialTheme.typography.headlineSmall)
                                                Spacer(Modifier.height(3.dp))
                                                Text(
                                                    "${server.username}@${server.host}:${server.port}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            MonitorStatusPill(monitorStatus)
                                        }
                                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                                            if (maxWidth < 520.dp) {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    WorkspaceButton("Open terminal", Icons.Rounded.Terminal, true) { onNavigateToTerminal(serverId) }
                                                    WorkspaceButton("Browse files", Icons.Rounded.Folder, false) { onNavigateToSftp(serverId) }
                                                }
                                            } else {
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Box(Modifier.weight(1f)) { WorkspaceButton("Open terminal", Icons.Rounded.Terminal, true) { onNavigateToTerminal(serverId) } }
                                                    Box(Modifier.weight(1f)) { WorkspaceButton("Browse files", Icons.Rounded.Folder, false) { onNavigateToSftp(serverId) } }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (monitorStatus == ServerMonitorStatus.CONNECTING || monitorStatus == ServerMonitorStatus.LOADING) {
                                item {
                                    CloudPanel(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(18.dp)) {
                                            Text("Connecting securely…", style = MaterialTheme.typography.titleSmall)
                                            Spacer(Modifier.height(10.dp))
                                            LinearProgressIndicator(Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }

                            if (monitorStatus == ServerMonitorStatus.ERROR) {
                                item {
                                    CloudPanel(Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                monitorError ?: "Live telemetry is unavailable.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            FilledTonalButton(onClick = viewModel::retry) { Text("Retry") }
                                        }
                                    }
                                }
                            }

                            if (monitorStatus == ServerMonitorStatus.ONLINE) {
                                item {
                                    CloudSectionHeader("Live telemetry", supportingText = "Updated about every three seconds")
                                }
                                item {
                                    ResponsiveTelemetry(
                                        cpuUsage = cpuUsage,
                                        ramUsage = ramUsage,
                                        cpuHistory = cpuHistory,
                                        ramHistory = ramHistory
                                    )
                                }
                                item {
                                    ResponsiveTelemetryStats(
                                        storage = storageUsage,
                                        latency = pingMs,
                                        loadAverage = loadAvg,
                                        uptime = uptime
                                    )
                                }
                                item { CloudSectionHeader("System", supportingText = "Reported by the remote host") }
                                item { SystemInformation(systemInfo) }

                                val availableCapabilities = capabilities.filterValues { it }.keys.sorted()
                                if (availableCapabilities.isNotEmpty()) {
                                    item { CloudSectionHeader("Detected tools", supportingText = "Available on this host") }
                                    item {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            availableCapabilities.forEach { capability ->
                                                StatusPill(capability, MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

private val ServerMonitorStatus.label: String
    get() = when (this) {
        ServerMonitorStatus.LOADING -> "Loading server"
        ServerMonitorStatus.CONNECTING -> "Connecting securely"
        ServerMonitorStatus.ONLINE -> "Online"
        ServerMonitorStatus.ERROR -> "Connection needs attention"
    }

@Composable
private fun MonitorStatusPill(status: ServerMonitorStatus) {
    val semantic = CloudLinkThemeValues.semanticColors
    val color = when (status) {
        ServerMonitorStatus.ONLINE -> semantic.success
        ServerMonitorStatus.CONNECTING, ServerMonitorStatus.LOADING -> semantic.warning
        ServerMonitorStatus.ERROR -> MaterialTheme.colorScheme.error
    }
    StatusPill(label = status.label, color = color, leadingContent = {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
    })
}

@Composable
private fun WorkspaceButton(title: String, icon: ImageVector, primary: Boolean, onClick: () -> Unit) {
    if (primary) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(title)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(title)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ResponsiveTelemetry(
    cpuUsage: Float?,
    ramUsage: Float?,
    cpuHistory: List<Float>,
    ramHistory: List<Float>
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cpuColor = if ((cpuUsage ?: 0f) >= 85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        val ramColor = if ((ramUsage ?: 0f) >= 85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryChart("CPU", cpuUsage, cpuHistory, cpuColor)
                TelemetryChart("Memory", ramUsage, ramHistory, ramColor)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { TelemetryChart("CPU", cpuUsage, cpuHistory, cpuColor) }
                Box(Modifier.weight(1f)) { TelemetryChart("Memory", ramUsage, ramHistory, ramColor) }
            }
        }
    }
}

@Composable
private fun TelemetryChart(title: String, value: Float?, history: List<Float>, color: Color) {
    CloudPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    value?.let { "${it.toInt()}%" } ?: "Unavailable",
                    style = if (value == null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else color
                )
            }
            Spacer(Modifier.height(10.dp))
            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                    Text("No samples", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LineChart(data = history, lineColor = color, modifier = Modifier.fillMaxWidth().height(76.dp))
            }
        }
    }
}

@Composable
private fun TelemetryStat(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    CloudPanel(modifier.widthIn(min = 145.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun ResponsiveTelemetryStats(
    storage: String,
    latency: String,
    loadAverage: String,
    uptime: String
) {
    val entries = listOf(
        Triple("Storage", storage, Icons.Rounded.SdStorage),
        Triple("SSH round trip", latency, Icons.Rounded.Wifi),
        Triple("Load average", loadAverage, Icons.Rounded.Speed),
        Triple("Uptime", uptime, Icons.Rounded.Timer)
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.forEach { (title, value, icon) ->
                    TelemetryStat(title, value, icon, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.chunked(2).forEach { rowEntries ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowEntries.forEach { (title, value, icon) ->
                            TelemetryStat(title, value, icon, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemInformation(info: SystemInfo) {
    val entries = listOf(
        "Hostname" to info.hostname,
        "Operating system" to info.os,
        "Kernel" to info.kernel,
        "Architecture" to info.arch,
        "CPU" to info.cpuModel,
        "Cores" to info.coreCount
    )
    CloudPanel(Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            maxItemsInEachRow = 2
        ) {
            entries.forEach { (label, value) ->
                Column(Modifier.weight(1f).widthIn(min = 140.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(3.dp))
                    Text(value.ifBlank { "Unavailable" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
