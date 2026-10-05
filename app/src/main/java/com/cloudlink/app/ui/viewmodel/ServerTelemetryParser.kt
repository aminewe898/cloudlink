package com.cloudlink.app.ui.viewmodel

data class SystemInfo(
    val hostname: String = "Unknown",
    val os: String = "Unknown",
    val kernel: String = "Unknown",
    val arch: String = "Unknown",
    val cpuModel: String = "Unknown",
    val coreCount: String = "Unknown",
    val sshVersion: String = "Unknown"
)

internal data class TelemetrySample(
    val cpuUsage: Float?,
    val ramUsage: Float?,
    val uptime: String,
    val storageUsage: String,
    val loadAverage: String
)

internal fun parseSystemInfo(output: String): SystemInfo? {
    if ("CL_HOST=" in output) {
        val values = parseMarkedValues(output)
        return SystemInfo(
            hostname = values["CL_HOST"].orUnavailable(),
            os = values["CL_OS"].orUnavailable(),
            kernel = values["CL_KERNEL"].orUnavailable(),
            arch = values["CL_ARCH"].orUnavailable(),
            cpuModel = values["CL_CPU_MODEL"].orUnavailable(),
            coreCount = values["CL_CORES"].orUnavailable(),
            sshVersion = "Unavailable"
        )
    }
    val values = output.split("---").map { it.trim() }
    if (values.size < 7) return null
    return SystemInfo(
        hostname = values[0].ifBlank { "Unavailable" },
        os = values[1].ifBlank { "Unavailable" },
        kernel = values[2].ifBlank { "Unavailable" },
        arch = values[3].ifBlank { "Unavailable" },
        cpuModel = values[4].ifBlank { "Unavailable" },
        coreCount = values[5].ifBlank { "Unavailable" },
        sshVersion = values[6].ifBlank { "Unavailable" }
    )
}

internal fun parseTelemetry(output: String): TelemetrySample? {
    if ("CL_CPU=" in output || "CL_RAM=" in output) {
        val values = parseMarkedValues(output)
        val uptimeSeconds = values["CL_UPTIME"]?.substringBefore(' ')?.toDoubleOrNull()
        return TelemetrySample(
            cpuUsage = values["CL_CPU"]?.toFloatOrNull()?.coerceIn(0f, 100f),
            ramUsage = values["CL_RAM"]?.toFloatOrNull()?.coerceIn(0f, 100f),
            uptime = uptimeSeconds?.let(::formatUptimeSeconds) ?: "Unavailable",
            storageUsage = values["CL_STORAGE"].orUnavailable(),
            loadAverage = values["CL_LOAD"].orUnavailable()
        )
    }
    val values = output.split("---").map { it.trim() }
    if (values.size < 5) return null
    val cpu = values[0].toFloatOrNull()?.coerceIn(0f, 100f)
    val ram = values[1].toFloatOrNull()?.coerceIn(0f, 100f)
    return TelemetrySample(
        cpuUsage = cpu,
        ramUsage = ram,
        uptime = values[2].ifBlank { "Unavailable" },
        storageUsage = values[3].ifBlank { "Unavailable" },
        loadAverage = values[4].ifBlank { "Unavailable" }
    )
}

private fun parseMarkedValues(output: String): Map<String, String> = output.lineSequence()
    .map { it.trim() }
    .filter { it.startsWith("CL_") && '=' in it }
    .associate { line -> line.substringBefore('=') to line.substringAfter('=').trim().trim('"') }

private fun String?.orUnavailable(): String = this?.takeIf { it.isNotBlank() } ?: "Unavailable"

private fun formatUptimeSeconds(seconds: Double): String {
    val totalMinutes = (seconds / 60).toLong().coerceAtLeast(0)
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return buildList {
        if (days > 0) add("${days}d")
        if (hours > 0 || days > 0) add("${hours}h")
        add("${minutes}m")
    }.joinToString(" ")
}
