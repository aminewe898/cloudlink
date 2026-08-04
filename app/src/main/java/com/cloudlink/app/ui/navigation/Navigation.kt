package com.cloudlink.app.ui.navigation

sealed class Screen(val route: String) {
    object Lock : Screen("lock")
    object Dashboard : Screen("dashboard")
    object Sessions : Screen("sessions")

    object ServerDetail : Screen("server_detail/{serverId}") {
        fun createRoute(serverId: Int) = "server_detail/$serverId"
    }

    object Terminal : Screen("terminal/{serverId}") {
        fun createRoute(serverId: Int) = "terminal/$serverId"
    }

    object Sftp : Screen("sftp/{serverId}") {
        fun createRoute(serverId: Int) = "sftp/$serverId"
    }

    object CodeEditor : Screen("code_editor/{serverId}?path={path}") {
        fun createRoute(serverId: Int, path: String) =
            "code_editor/$serverId?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object Tools : Screen("tools")
    object Settings : Screen("settings")
}
