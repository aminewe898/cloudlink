package com.cloudlink.app.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalBufferTest {
    @Test
    fun `parser applies cursor movement and colors`() {
        val buffer = TerminalBuffer(cols = 8, rows = 3)
        val parser = Vt100Parser(buffer)

        parser.process("abc\u001B[2D\u001B[31mZ")

        val snapshot = buffer.getSnapshot()
        assertEquals('a'.code, TerminalCell(snapshot.screen[0][0]).codePoint)
        assertEquals('Z'.code, TerminalCell(snapshot.screen[0][1]).codePoint)
        assertEquals(1, TerminalCell(snapshot.screen[0][1]).fgIndex)
    }

    @Test
    fun `parser preserves supplementary unicode code points`() {
        val buffer = TerminalBuffer(cols = 4, rows = 2)
        Vt100Parser(buffer).process("A\uD83D\uDE80B")

        val snapshot = buffer.getSnapshot()
        assertEquals('A'.code, TerminalCell(snapshot.screen[0][0]).codePoint)
        assertEquals(0x1F680, TerminalCell(snapshot.screen[0][1]).codePoint)
        assertEquals('B'.code, TerminalCell(snapshot.screen[0][2]).codePoint)
    }

    @Test
    fun `alternate screen does not pollute scrollback`() {
        val buffer = TerminalBuffer(cols = 4, rows = 2)
        val parser = Vt100Parser(buffer)

        parser.process("one\ntwo\n")
        val before = buffer.getSnapshot().scrollback.size
        parser.process("\u001B[?1049hfull\nview\n\u001B[?1049l")

        val snapshot = buffer.getSnapshot()
        assertEquals(before, snapshot.scrollback.size)
        assertFalse(snapshot.fullRedraw && snapshot.screen.isEmpty())
        assertTrue(snapshot.rows == 2)
    }

    @Test
    fun `clearing history preserves negotiated paste mode`() {
        val buffer = TerminalBuffer(cols = 8, rows = 2)
        val parser = Vt100Parser(buffer)

        parser.process("\u001B[?2004h")
        repeat(5) { parser.process("line$it\r\n") }
        assertTrue(buffer.getSnapshot().scrollback.isNotEmpty())

        buffer.clearHistory()

        val snapshot = buffer.getSnapshot()
        assertTrue(snapshot.bracketedPasteMode)
        assertTrue(snapshot.scrollback.isEmpty())
    }

    @Test
    fun `alternate screen 1049 restores cursor attributes and main content`() {
        val buffer = TerminalBuffer(cols = 8, rows = 3)
        val parser = Vt100Parser(buffer)

        parser.process("main\u001B[2;3H\u001B[31m\u001B[?1049halt\u001B[?1049lX")

        val snapshot = buffer.getSnapshot()
        assertEquals('m'.code, TerminalCell(snapshot.screen[0][0]).codePoint)
        assertEquals('X'.code, TerminalCell(snapshot.screen[1][2]).codePoint)
        assertEquals(1, TerminalCell(snapshot.screen[1][2]).fgIndex)
    }

    @Test
    fun `erase saved lines keeps visible screen`() {
        val buffer = TerminalBuffer(cols = 5, rows = 2)
        val parser = Vt100Parser(buffer)
        repeat(4) { parser.process("$it\r\n") }
        assertTrue(buffer.getSnapshot().scrollback.isNotEmpty())

        parser.process("\u001B[3J")

        val snapshot = buffer.getSnapshot()
        assertTrue(snapshot.scrollback.isEmpty())
        assertEquals('3'.code, TerminalCell(snapshot.screen[0][0]).codePoint)
    }

    @Test
    fun `multiple private modes are applied together`() {
        val buffer = TerminalBuffer(cols = 5, rows = 2)
        val parser = Vt100Parser(buffer)

        parser.process("\u001B[?25;2004l")
        var snapshot = buffer.getSnapshot()
        assertFalse(snapshot.cursorVisible)
        assertFalse(snapshot.bracketedPasteMode)

        parser.process("\u001B[?25;2004h")
        snapshot = buffer.getSnapshot()
        assertTrue(snapshot.cursorVisible)
        assertTrue(snapshot.bracketedPasteMode)
    }

    @Test
    fun `true color input is approximated without changing text attributes`() {
        val buffer = TerminalBuffer(cols = 5, rows = 2)
        Vt100Parser(buffer).process("\u001B[38;2;255;0;0mR")

        val cell = TerminalCell(buffer.getSnapshot().screen[0][0])
        assertFalse(cell.isFgDefault)
        assertFalse(cell.isDim)
        assertEquals('R'.code, cell.codePoint)
    }

    @Test
    fun `split surrogate pair is preserved across parser chunks`() {
        val buffer = TerminalBuffer(cols = 5, rows = 2)
        val parser = Vt100Parser(buffer)
        parser.process("\uD83D")
        parser.process("\uDE80")

        assertEquals(0x1F680, TerminalCell(buffer.getSnapshot().screen[0][0]).codePoint)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `terminal rejects non positive dimensions`() {
        TerminalBuffer(cols = 0, rows = 2)
    }

    @Test
    fun `insert mode shifts existing characters`() {
        val buffer = TerminalBuffer(cols = 6, rows = 2)
        val parser = Vt100Parser(buffer)
        parser.process("abcd\u001B[3G\u001B[4hX\u001B[4l")

        val text = buffer.getSnapshot().screen[0]
            .joinToString("") { TerminalCell(it).text }
        assertTrue(text.startsWith("abXcd"))
        assertFalse(buffer.insertMode)
    }

    @Test
    fun `device status reports use one based cursor position`() {
        val buffer = TerminalBuffer(cols = 8, rows = 3)
        val responses = mutableListOf<String>()
        val parser = Vt100Parser(buffer, responses::add)
        parser.process("\u001B[2;4H\u001B[5n\u001B[6n\u001B[c")

        assertEquals(listOf("\u001B[0n", "\u001B[2;4R", "\u001B[?1;2c"), responses)
    }

    @Test
    fun `application cursor mode is tracked`() {
        val buffer = TerminalBuffer(cols = 8, rows = 3)
        val parser = Vt100Parser(buffer)
        parser.process("\u001B[?1h")
        assertTrue(buffer.applicationCursorKeys)
        parser.process("\u001B[?1l")
        assertFalse(buffer.applicationCursorKeys)
    }
}
