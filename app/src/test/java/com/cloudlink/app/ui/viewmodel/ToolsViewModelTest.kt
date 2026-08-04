package com.cloudlink.app.ui.viewmodel

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsViewModelTest {

    @Test
    fun `host validation accepts hostnames and ip addresses`() {
        assertTrue(isValidHostOrIp("server.local"))
        assertTrue(isValidHostOrIp("192.168.1.25"))
        assertTrue(isValidHostOrIp("2001:db8::10"))
        assertFalse(isValidHostOrIp("bad host"))
        assertFalse(isValidHostOrIp("server..local"))
    }

    @Test
    fun `mac parser accepts common separators`() {
        val expected = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        assertArrayEquals(expected, parseMacAddress("00:11:22:33:44:55"))
        assertArrayEquals(expected, parseMacAddress("00-11-22-33-44-55"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mac parser rejects incomplete address`() {
        parseMacAddress("00:11:22:33")
    }
}
