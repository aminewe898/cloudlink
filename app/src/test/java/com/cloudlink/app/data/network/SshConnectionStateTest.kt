package com.cloudlink.app.data.network

import com.jcraft.jsch.JSchException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SshConnectionStateTest {
    @Test
    fun `authentication errors are mapped without leaking transport detail`() {
        val mapped = mapSshException(JSchException("Auth fail"))

        assertEquals(SshFailureKind.AUTHENTICATION, mapped.kind)
        assertEquals("Authentication failed. Check the username and saved credential.", mapped.message)
        assertFalse(mapped.message.orEmpty().contains("Auth fail"))
    }

    @Test
    fun `host verification errors fail into host key category`() {
        val mapped = mapSshException(JSchException("reject HostKey: example"))

        assertEquals(SshFailureKind.HOST_KEY, mapped.kind)
    }

    @Test
    fun `timeouts include an actionable message`() {
        val mapped = mapSshException(JSchException("timeout: socket is not established"))

        assertEquals(SshFailureKind.TIMEOUT, mapped.kind)
        assertEquals("The SSH connection timed out. Check the address, port, and network.", mapped.message)
    }
}
