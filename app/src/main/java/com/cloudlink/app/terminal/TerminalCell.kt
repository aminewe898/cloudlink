package com.cloudlink.app.terminal

/**
 * A highly optimized value class for a single terminal cell to avoid GC pressure.
 * Packs a UTF-32 character and its styling attributes into a single 64-bit Long.
 *
 * Bit layout:
 * [63-43] Char (21 bits) - Unicode codepoint (up to 0x10FFFF)
 * [42-33] FG color (10 bits) - (0-255 color index, bit 9 is 'is_default' flag)
 * [32-23] BG color (10 bits) - (0-255 color index, bit 9 is 'is_default' flag)
 * [22] Bold
 * [21] Italic
 * [20] Underline
 * [19] Reverse
 * [18] Dim
 * [17] Blink
 * [16] Hidden
 */
@JvmInline
value class TerminalCell(val encoded: Long) {
    val codePoint: Int
        get() = ((encoded ushr 43).toInt() and 0x1FFFFF).let { if (it == 0) ' '.code else it }

    val text: String
        get() = String(Character.toChars(codePoint.coerceIn(0, Character.MAX_CODE_POINT)))

    @Deprecated("Use codePoint or text so supplementary Unicode characters are preserved")
    val char: Char get() = text.first()

    val fgColor: Int get() = (encoded ushr 33).toInt() and 0x3FF
    val bgColor: Int get() = (encoded ushr 23).toInt() and 0x3FF

    val isFgDefault: Boolean get() = (fgColor and 0x200) != 0
    val isBgDefault: Boolean get() = (bgColor and 0x200) != 0
    val fgIndex: Int get() = fgColor and 0xFF
    val bgIndex: Int get() = bgColor and 0xFF

    val isBold: Boolean get() = (encoded and (1L shl 22)) != 0L
    val isItalic: Boolean get() = (encoded and (1L shl 21)) != 0L
    val isUnderline: Boolean get() = (encoded and (1L shl 20)) != 0L
    val isReverse: Boolean get() = (encoded and (1L shl 19)) != 0L
    val isDim: Boolean get() = (encoded and (1L shl 18)) != 0L
    val isBlink: Boolean get() = (encoded and (1L shl 17)) != 0L
    val isHidden: Boolean get() = (encoded and (1L shl 16)) != 0L

    companion object {
        val BLANK = create(' ', -1, -1)

        fun create(
            char: Char,
            fgIndex: Int = -1, // -1 means default
            bgIndex: Int = -1, // -1 means default
            bold: Boolean = false,
            italic: Boolean = false,
            underline: Boolean = false,
            reverse: Boolean = false,
            dim: Boolean = false,
            blink: Boolean = false,
            hidden: Boolean = false
        ): TerminalCell = create(
            codePoint = char.code,
            fgIndex = fgIndex,
            bgIndex = bgIndex,
            bold = bold,
            italic = italic,
            underline = underline,
            reverse = reverse,
            dim = dim,
            blink = blink,
            hidden = hidden
        )

        fun create(
            codePoint: Int,
            fgIndex: Int = -1,
            bgIndex: Int = -1,
            bold: Boolean = false,
            italic: Boolean = false,
            underline: Boolean = false,
            reverse: Boolean = false,
            dim: Boolean = false,
            blink: Boolean = false,
            hidden: Boolean = false
        ): TerminalCell {
            require(Character.isValidCodePoint(codePoint)) { "Invalid Unicode code point: $codePoint" }
            val cp = codePoint.toLong() and 0x1FFFFFL

            val fg = if (fgIndex == -1) 0x200L else (fgIndex.toLong() and 0xFFL)
            val bg = if (bgIndex == -1) 0x200L else (bgIndex.toLong() and 0xFFL)

            var attrs = 0L
            if (bold) attrs = attrs or (1L shl 22)
            if (italic) attrs = attrs or (1L shl 21)
            if (underline) attrs = attrs or (1L shl 20)
            if (reverse) attrs = attrs or (1L shl 19)
            if (dim) attrs = attrs or (1L shl 18)
            if (blink) attrs = attrs or (1L shl 17)
            if (hidden) attrs = attrs or (1L shl 16)

            return TerminalCell((cp shl 43) or (fg shl 33) or (bg shl 23) or attrs)
        }

        fun createFrom(
            char: Char,
            baseCell: TerminalCell
        ): TerminalCell = createFrom(char.code, baseCell)

        fun createFrom(
            codePoint: Int,
            baseCell: TerminalCell
        ): TerminalCell {
            require(Character.isValidCodePoint(codePoint)) { "Invalid Unicode code point: $codePoint" }
            val cp = codePoint.toLong() and 0x1FFFFFL
            val attrsOnly = baseCell.encoded and 0x000007FFFFFFFFFFL
            return TerminalCell((cp shl 43) or attrsOnly)
        }
    }
}
