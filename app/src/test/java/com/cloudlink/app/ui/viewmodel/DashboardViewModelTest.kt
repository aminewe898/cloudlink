package com.cloudlink.app.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardViewModelTest {

    @Test
    fun `telemetry parser returns bounded values`() {
        val sample = requireNotNull(parseTelemetry("125\n---\n42.5\n---\nup 2 days\n---\n61%\n---\n0.10 0.20 0.30"))

        assertEquals(100f, requireNotNull(sample.cpuUsage), 0f)
        assertEquals(42.5f, requireNotNull(sample.ramUsage), 0f)
        assertEquals("61%", sample.storageUsage)
    }

    @Test
    fun `telemetry parser rejects incomplete output`() {
        assertNull(parseTelemetry("10\n---\n20"))
    }

    @Test
    fun `system info parser maps all fields`() {
        val info = requireNotNull(parseSystemInfo("lab\n---\nUbuntu\n---\n6.8\n---\nx86_64\n---\nRyzen\n---\n8\n---\nOpenSSH"))

        assertEquals("lab", info.hostname)
        assertEquals("Ubuntu", info.os)
        assertEquals("8", info.coreCount)
    }

    @Test
    fun `marked telemetry keeps available metrics when cpu is missing`() {
        val sample = requireNotNull(
            parseTelemetry(
                "CL_CPU=\nCL_RAM=37.25\nCL_UPTIME=90061.2\nCL_STORAGE=72%\nCL_LOAD=0.10 0.20 0.30"
            )
        )

        assertNull(sample.cpuUsage)
        assertEquals(37.25f, requireNotNull(sample.ramUsage), 0f)
        assertEquals("1d 1h 1m", sample.uptime)
        assertEquals("72%", sample.storageUsage)
    }

    @Test
    fun `marked system info tolerates missing commands per field`() {
        val info = requireNotNull(
            parseSystemInfo("CL_HOST=router\nCL_OS=OpenWrt\nCL_KERNEL=\nCL_ARCH=mips\nCL_CPU_MODEL=\nCL_CORES=1")
        )

        assertEquals("router", info.hostname)
        assertEquals("Unavailable", info.kernel)
        assertEquals("Unavailable", info.cpuModel)
    }
}
