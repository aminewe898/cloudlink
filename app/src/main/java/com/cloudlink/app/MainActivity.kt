package com.cloudlink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cloudlink.app.ui.navigation.Screen
import com.cloudlink.app.ui.components.CloudPrimaryNavigation
import com.cloudlink.app.data.network.HostKeyPromptCoordinator
import com.cloudlink.app.ui.theme.CloudLinkTheme
import com.cloudlink.app.ui.theme.ThemeManager
import com.cloudlink.app.ui.viewmodel.ServerListViewModel
import com.cloudlink.app.ui.viewmodel.SessionsViewModel
import com.cloudlink.app.ui.viewmodel.TerminalViewModel
import com.cloudlink.app.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var hostKeyPromptCoordinator: HostKeyPromptCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeState by themeManager.currentTheme.collectAsStateWithLifecycle(initialValue = null)
            val secureScreen by themeManager.secureScreen.collectAsStateWithLifecycle(initialValue = true)

            LaunchedEffect(secureScreen) {
                if (secureScreen) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            themeState?.let { currentTheme ->
                CloudLinkTheme(appThemeType = currentTheme) {
                    CloudLinkApp(hostKeyPromptCoordinator)
                }
            }
        }
    }
}

@Composable
fun CloudLinkApp(hostKeyPromptCoordinator: HostKeyPromptCoordinator) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Lock.route
    val hostKeyPrompt by hostKeyPromptCoordinator.pendingPrompt.collectAsStateWithLifecycle()

    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val primaryRoutes = setOf(Screen.Dashboard.route, Screen.Sessions.route, Screen.Tools.route)
    val showBottomBar = currentRoute in primaryRoutes && !isKeyboardVisible

    fun navigatePrimary(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var backgroundTime by remember { mutableLongStateOf(0L) }
    val timeoutMillis = 5 * 60 * 1000L // 5 minutes

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                backgroundTime = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_START) {
                if (backgroundTime > 0 && System.currentTimeMillis() - backgroundTime > timeoutMillis) {
                    navController.navigate(Screen.Lock.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CloudPrimaryNavigation(
                    currentRoute = currentRoute,
                    onServers = { navigatePrimary(Screen.Dashboard.route) },
                    onSessions = { navigatePrimary(Screen.Sessions.route) },
                    onTools = { navigatePrimary(Screen.Tools.route) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Lock.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lock.route) {
                LockScreen(
                    onUnlock = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Lock.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                val serverListViewModel: ServerListViewModel = hiltViewModel()
                ServersScreen(
                    serverListViewModel = serverListViewModel,
                    onNavigateToTerminal = { serverId ->
                        navController.navigate(Screen.ServerDetail.createRoute(serverId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Sessions.route) {
                val sessionsViewModel: SessionsViewModel = hiltViewModel()
                SessionsScreen(
                    viewModel = sessionsViewModel,
                    onNavigateToServers = { navigatePrimary(Screen.Dashboard.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenSession = { serverId -> navController.navigate(Screen.Terminal.createRoute(serverId)) }
                )
            }
            composable(
                route = Screen.ServerDetail.route,
                arguments = listOf(androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                val dashboardViewModel: com.cloudlink.app.ui.viewmodel.DashboardViewModel = hiltViewModel()
                ServerDetailScreen(
                    serverId = serverId,
                    viewModel = dashboardViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTerminal = { id -> navController.navigate(Screen.Terminal.createRoute(id)) },
                    onNavigateToSftp = { id -> navController.navigate(Screen.Sftp.createRoute(id)) }
                )
            }
            composable(
                route = Screen.Terminal.route,
                arguments = listOf(androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                val context = LocalContext.current
                val terminalViewModel: TerminalViewModel = hiltViewModel(context as ComponentActivity)
                TerminalScreen(
                    viewModel = terminalViewModel,
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Sftp.route,
                arguments = listOf(androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                val context = LocalContext.current
                val sftpViewModel: com.cloudlink.app.ui.viewmodel.SftpViewModel = hiltViewModel(context as ComponentActivity)
                FileManagerScreen(
                    viewModel = sftpViewModel,
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFile = { path ->
                        navController.navigate(Screen.CodeEditor.createRoute(serverId, path))
                    }
                )
            }
            composable(
                route = Screen.CodeEditor.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("path") { type = androidx.navigation.NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                val path = backStackEntry.arguments?.getString("path") ?: ""
                val context = LocalContext.current
                val sftpViewModel: com.cloudlink.app.ui.viewmodel.SftpViewModel = hiltViewModel(context as ComponentActivity)
                CodeEditorScreen(
                    serverId = serverId,
                    filePath = path,
                    viewModel = sftpViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: com.cloudlink.app.ui.viewmodel.SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    hostKeyPrompt?.takeIf { currentRoute != Screen.Lock.route }?.let { prompt ->
        AlertDialog(
            onDismissRequest = { hostKeyPromptCoordinator.respond(prompt.id, false) },
            title = { Text("Verify SSH host key") },
            text = {
                Text(
                    text = prompt.message.ifBlank {
                        "The server presented a host key that has not been trusted on this device."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { hostKeyPromptCoordinator.respond(prompt.id, true) }) {
                    Text("Trust this host")
                }
            },
            dismissButton = {
                TextButton(onClick = { hostKeyPromptCoordinator.respond(prompt.id, false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
