package com.cloudlink.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerFormValidationTest {
    @Test
    fun `server host validation accepts dns ipv4 and ipv6`() {
        assertTrue(isValidServerHost("server.local"))
        assertTrue(isValidServerHost("192.0.2.10"))
        assertTrue(isValidServerHost("2001:db8::10"))
    }

    @Test
    fun `server host validation rejects whitespace and empty labels`() {
        assertFalse(isValidServerHost("bad host"))
        assertFalse(isValidServerHost("server..local"))
        assertFalse(isValidServerHost(""))
    }
}
