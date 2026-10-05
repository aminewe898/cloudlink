package com.cloudlink.app.terminal

import kotlin.math.max

class Vt100Parser(
    private val buffer: TerminalBuffer,
    private val responseSink: (String) -> Unit = {}
) {

    private enum class State {
        GROUND, ESCAPE, CSI_ENTRY, CSI_PARAM, OSC_STRING
    }

    private var state = State.GROUND
    private val params = mutableListOf<Int>()
    private var currentParam = 0
    private var hasParam = false
    private var privateMode = false
    private val oscBuffer = StringBuilder()
    private var pendingHighSurrogate: Char? = null

    fun process(text: String) {
        var normalizedText = pendingHighSurrogate?.let { it + text } ?: text
        pendingHighSurrogate = null
        if (normalizedText.lastOrNull()?.isHighSurrogate() == true) {
            pendingHighSurrogate = normalizedText.last()
            normalizedText = normalizedText.dropLast(1)
        }

        var i = 0
        while (i < normalizedText.length) {
            val cp = normalizedText.codePointAt(i)
            i += Character.charCount(cp)
            processCodePoint(cp)
        }
    }

    private fun processCodePoint(cp: Int) {
        val c = cp.toChar()

        // Handle structural controls regardless of state
        if (cp == 0x18 || cp == 0x1A) { // CAN or SUB
            state = State.GROUND
            return
        }
        if (cp == 0x1B) { // ESC
            state = State.ESCAPE
            return
        }
        if (cp == 0x9B) { // CSI (8-bit)
            state = State.CSI_ENTRY
            params.clear()
            currentParam = 0
            hasParam = false
            privateMode = false
            return
        }
        if (cp == 0x9D) { // OSC (8-bit)
            state = State.OSC_STRING
            oscBuffer.clear()
            return
        }

        when (state) {
            State.GROUND -> {
                when (cp) {
                    0x00 -> {} // NUL ignored
                    0x07 -> {} // BEL ignored/beep
                    0x08 -> buffer.backspace()
                    0x09 -> buffer.tab()
                    0x0A, 0x0B, 0x0C -> buffer.lineFeed()
                    0x0D -> buffer.carriageReturn()
                    else -> if (cp >= 0x20) {
                        buffer.putCodePoint(cp)
                    }
                }
            }
            State.ESCAPE -> {
                when (cp) {
                    0x5B -> { // '['
                        state = State.CSI_ENTRY
                        params.clear()
                        currentParam = 0
                        hasParam = false
                        privateMode = false
                    }
                    0x5D -> { // ']'
                        state = State.OSC_STRING
                        oscBuffer.clear()
                    }
                    0x37 -> { // '7' Save cursor
                        buffer.saveCursor()
                        state = State.GROUND
                    }
                    0x38 -> { // '8' Restore cursor
                        buffer.restoreCursor()
                        state = State.GROUND
                    }
                    0x4D -> { // 'M' Reverse index
                        buffer.reverseIndex()
                        state = State.GROUND
                    }
                    0x44 -> { // 'D' Index (Line feed)
                        buffer.lineFeed()
                        state = State.GROUND
                    }
                    0x45 -> { // 'E' Next line
                        buffer.carriageReturn()
                        buffer.lineFeed()
                        state = State.GROUND
                    }
                    0x63 -> { // 'c' Reset
                        buffer.reset()
                        state = State.GROUND
                    }
                    else -> state = State.GROUND // ignore other ESC sequences for now
                }
            }
            State.CSI_ENTRY, State.CSI_PARAM -> {
                if (cp in 0x30..0x39) { // '0'-'9'
                    state = State.CSI_PARAM
                    hasParam = true
                    currentParam = (currentParam * 10 + (cp - 0x30)).coerceAtMost(MAX_PARAMETER_VALUE)
                } else if (cp == 0x3B || cp == 0x3A) { // ';' or ':'
                    params.add(currentParam)
                    currentParam = 0
                    hasParam = false
                    state = State.CSI_PARAM
                } else if (cp == 0x3F && state == State.CSI_ENTRY) { // '?'
                    privateMode = true
                } else if (cp in 0x40..0x7E) { // Dispatch
                    if (hasParam || params.isNotEmpty()) {
                        params.add(currentParam)
                    }
                    dispatchCSI(c)
                    state = State.GROUND
                } else {
                    // Invalid/ignore
                }
            }
            State.OSC_STRING -> {
                if (cp == 0x07 || cp == 0x9C || (cp == 0x5C && oscBuffer.endsWith("\u001B"))) { // BEL or ST
                    state = State.GROUND
                } else if (cp >= 0x20 || cp == 0x1B) {
                    if (oscBuffer.length < MAX_OSC_LENGTH) oscBuffer.append(c)
                }
            }
        }
    }

    private fun dispatchCSI(cmd: Char) {
        val p = params.ifEmpty { listOf(0) }
        val p1 = max(1, p.getOrElse(0) { 1 })

        when (cmd) {
            'A' -> buffer.moveCursorRelative(-p1, 0) // Up
            'B' -> buffer.moveCursorRelative(p1, 0)  // Down
            'C' -> buffer.moveCursorRelative(0, p1)  // Right
            'D' -> buffer.moveCursorRelative(0, -p1) // Left
            'E' -> { buffer.carriageReturn(); buffer.moveCursorRelative(p1, 0) } // Next line
            'F' -> { buffer.carriageReturn(); buffer.moveCursorRelative(-p1, 0) } // Prev line
            'G' -> buffer.moveCursorAbsolute(buffer.cursorY, p.getOrElse(0) { 1 } - 1) // Column abs
            'H', 'f' -> { // Cursor position (1-indexed)
                val row = max(1, p.getOrElse(0) { 1 }) - 1
                val col = max(1, p.getOrElse(1) { 1 }) - 1
                buffer.moveCursorAbsolute(row, col)
            }
            'J' -> buffer.eraseInDisplay(p.getOrElse(0) { 0 })
            'K' -> buffer.eraseInLine(p.getOrElse(0) { 0 })
            'L' -> buffer.insertLines(p1) // IL
            'M' -> buffer.deleteLines(p1) // DL
            '@' -> buffer.insertChars(p1) // ICH
            'P' -> buffer.deleteChars(p1) // DCH
            'S' -> buffer.scrollUp(p1)
            'T' -> buffer.scrollDown(p1)
            'r' -> { // Set margins DECSTBM
                val top = max(1, p.getOrElse(0) { 1 }) - 1
                val bottom = max(1, p.getOrElse(1) { buffer.rows }) - 1
                buffer.setScrollRegion(top, bottom)
            }
            'm' -> applySgr(p)
            'n' -> when (p.getOrElse(0) { 0 }) {
                5 -> responseSink("\u001B[0n")
                6 -> responseSink("\u001B[${buffer.cursorY + 1};${buffer.cursorX + 1}R")
            }
            'c' -> responseSink("\u001B[?1;2c")
            'h', 'l' -> { // Set/Reset Mode
                val enable = cmd == 'h'
                if (privateMode) {
                    p.forEach { mode ->
                        when (mode) {
                            1 -> buffer.setApplicationCursorKeys(enable) // DECCKM
                            7 -> buffer.setAutoWrap(enable) // DECAWM
                            25 -> buffer.setCursorVisible(enable) // DECTCEM
                            47 -> buffer.useAltScreen(enable, clearOnEnter = false)
                            1047 -> buffer.useAltScreen(enable, clearOnEnter = true)
                            1049 -> buffer.useAltScreen(enable, saveAndRestoreCursor = true, clearOnEnter = true)
                            2004 -> buffer.setBracketedPaste(enable)
                            1000, 1002, 1003, 1006 -> {} // Mouse tracking ignored/consumed safely
                        }
                    }
                } else {
                    p.forEach { mode ->
                        if (mode == 4) buffer.setInsertMode(enable)
                    }
                }
            }
            's' -> buffer.saveCursor() // ANSISYSSC
            'u' -> buffer.restoreCursor() // ANSISYSRC
        }
    }

    private fun applySgr(params: List<Int>) {
        if (params.isEmpty()) {
            buffer.currentAttrs = TerminalCell.BLANK.encoded
            return
        }

        var i = 0
        var fg = -1
        var bg = -1
        var bold = false
        var italic = false
        var underline = false
        var reverse = false
        var dim = false
        var blink = false
        var hidden = false

        // Decode existing attributes
        val current = TerminalCell(buffer.currentAttrs)
        fg = if (current.isFgDefault) -1 else current.fgIndex
        bg = if (current.isBgDefault) -1 else current.bgIndex
        bold = current.isBold
        italic = current.isItalic
        underline = current.isUnderline
        reverse = current.isReverse
        dim = current.isDim
        blink = current.isBlink
        hidden = current.isHidden

        while (i < params.size) {
            val p = params[i]
            when (p) {
                0 -> { fg = -1; bg = -1; bold = false; italic = false; underline = false; reverse = false; dim = false; blink = false; hidden = false }
                1 -> bold = true
                2 -> dim = true
                3 -> italic = true
                4 -> underline = true
                5 -> blink = true
                7 -> reverse = true
                8 -> hidden = true
                22 -> { bold = false; dim = false }
                23 -> italic = false
                24 -> underline = false
                25 -> blink = false
                27 -> reverse = false
                28 -> hidden = false
                in 30..37 -> fg = p - 30
                38 -> {
                    if (i + 2 < params.size && params[i+1] == 5) {
                        fg = params[i+2]
                        i += 2
                    } else if (i + 4 < params.size && params[i+1] == 2) {
                        val rgbStart = if (i + 5 < params.size && params[i + 2] == 0) i + 3 else i + 2
                        if (rgbStart + 2 < params.size) {
                            fg = rgbToXterm256(params[rgbStart], params[rgbStart + 1], params[rgbStart + 2])
                            i = rgbStart + 2
                        }
                    }
                }
                39 -> fg = -1
                in 40..47 -> bg = p - 40
                48 -> {
                    if (i + 2 < params.size && params[i+1] == 5) {
                        bg = params[i+2]
                        i += 2
                    } else if (i + 4 < params.size && params[i+1] == 2) {
                        val rgbStart = if (i + 5 < params.size && params[i + 2] == 0) i + 3 else i + 2
                        if (rgbStart + 2 < params.size) {
                            bg = rgbToXterm256(params[rgbStart], params[rgbStart + 1], params[rgbStart + 2])
                            i = rgbStart + 2
                        }
                    }
                }
                49 -> bg = -1
                in 90..97 -> fg = p - 90 + 8
                in 100..107 -> bg = p - 100 + 8
            }
            i++
        }
        buffer.currentAttrs = TerminalCell.create(' ', fg, bg, bold, italic, underline, reverse, dim, blink, hidden).encoded
    }

    private fun rgbToXterm256(red: Int, green: Int, blue: Int): Int {
        val r = red.coerceIn(0, 255)
        val g = green.coerceIn(0, 255)
        val b = blue.coerceIn(0, 255)
        if (r == g && g == b && r in 8..238) {
            return 232 + ((r - 8) / 10).coerceIn(0, 23)
        }
        fun cube(value: Int): Int = ((value / 255.0) * 5).toInt().coerceIn(0, 5)
        return 16 + 36 * cube(r) + 6 * cube(g) + cube(b)
    }

    private companion object {
        const val MAX_PARAMETER_VALUE = 9_999
        const val MAX_OSC_LENGTH = 4_096
    }
}
