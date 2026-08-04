package com.cloudlink.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.cloudlink.app.ui.components.CloudEmptyState

@Composable
fun DisconnectedStateView(
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CloudEmptyState(
            icon = Icons.Default.CloudOff,
            title = "Connection lost",
            supportingText = "Check the network connection, then reconnect to continue.",
            actionLabel = "Reconnect",
            onAction = onReconnect
        )
    }
}
