package com.cloudlink.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cloudlink.app.ui.navigation.Screen

@Composable
fun CloudPrimaryNavigation(
    currentRoute: String,
    onServers: () -> Unit,
    onSessions: () -> Unit,
    onTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentRoute == Screen.Dashboard.route,
                onClick = onServers,
                icon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
                label = { Text("Servers") },
                colors = colors,
                modifier = Modifier.testTag("nav_item_servers")
            )
            NavigationBarItem(
                selected = currentRoute == Screen.Sessions.route,
                onClick = onSessions,
                icon = { Icon(Icons.Rounded.Terminal, contentDescription = null) },
                label = { Text("Sessions") },
                colors = colors,
                modifier = Modifier.testTag("nav_item_sessions")
            )
            NavigationBarItem(
                selected = currentRoute == Screen.Tools.route,
                onClick = onTools,
                icon = { Icon(Icons.Rounded.Build, contentDescription = null) },
                label = { Text("Tools") },
                colors = colors,
                modifier = Modifier.testTag("nav_item_tools")
            )
        }
    }
}
