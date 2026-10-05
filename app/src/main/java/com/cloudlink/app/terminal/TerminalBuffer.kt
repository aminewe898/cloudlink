package com.cloudlink.app.terminal

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min

class TerminalBuffer(var cols: Int = 80, var rows: Int = 24) {

    init {
        require(cols > 0 && rows > 0) { "Terminal dimensions must be positive" }
    }

    // Main screen
    private var screen = Array(rows) { LongArray(cols) { TerminalCell.BLANK.encoded } }

    // Alternate screen (used by full-screen apps like vim, htop)
    private var altScreen = Array(rows) { LongArray(cols) { TerminalCell.BLANK.encoded } }

    // Scrollback buffer (max 10,000 lines)
    private val scrollback = kotlin.collections.ArrayDeque<LongArray>()
    private val MAX_SCROLLBACK = 10000
    private var scrollbackVersion = 0L
    private var cachedScrollbackVersion = -1L
    private var cachedScrollback: List<LongArray> = emptyList()

    // Cursor state
    var cursorX = 0
        private set
    var cursorY = 0
        private set
    var cursorVisible = true
        private set

    // Saved cursor state (for ESC 7/8 and CSI s/u)
    private var savedCursorX = 0
    private var savedCursorY = 0
    private var savedCurrentAttrs = TerminalCell.BLANK.encoded
    private var savedMainCursorX = 0
    private var savedMainCursorY = 0
    private var savedMainAttrs = TerminalCell.BLANK.encoded

    // Scroll region boundaries (0-indexed)
    private var scrollTop = 0
    private var scrollBottom = rows - 1

    // Terminal modes
    var useAltScreen = false
        private set
    var autoWrapEnabled = true
        private set
    var bracketedPasteMode = false
        private set
    var applicationCursorKeys = false
        private set
    var insertMode = false
        private set

    // Current writing attributes
    var currentAttrs = TerminalCell.BLANK.encoded

    // Internal state
    private var autoWrapPending = false
    private val lock = ReentrantLock()

    // Dirty region tracking to optimize rendering
    // Represents a bounding box of changed cells since last snapshot
    private var dirtyMinX = cols
    private var dirtyMinY = rows
    private var dirtyMaxX = -1
    private var dirtyMaxY = -1
    private var fullRedrawRequired = true

    init { reset() }

    private fun markDirty(x: Int, y: Int) {
        if (x < dirtyMinX) dirtyMinX = x
        if (y < dirtyMinY) dirtyMinY = y
        if (x > dirtyMaxX) dirtyMaxX = x
        if (y > dirtyMaxY) dirtyMaxY = y
    }

    private fun markRowDirty(y: Int) {
        markDirty(0, y)
        markDirty(cols - 1, y)
    }

    private fun markFullDirty() {
        fullRedrawRequired = true
    }

    fun reset() = lock.withLock {
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                screen[y][x] = TerminalCell.BLANK.encoded
                altScreen[y][x] = TerminalCell.BLANK.encoded
            }
        }
        scrollback.clear()
        scrollbackVersion++

        cursorX = 0
        cursorY = 0
        cursorVisible = true

        savedCursorX = 0
        savedCursorY = 0
        savedCurrentAttrs = TerminalCell.BLANK.encoded

        scrollTop = 0
        scrollBottom = rows - 1

        useAltScreen = false
        autoWrapEnabled = true
        bracketedPasteMode = false
        applicationCursorKeys = false
        insertMode = false
        autoWrapPending = false

        currentAttrs = TerminalCell.BLANK.encoded
        markFullDirty()
    }

    /** Clears visible content and scrollback without losing modes negotiated by the remote app. */
    fun clearHistory() = lock.withLock {
        val preserveAltScreen = useAltScreen
        val preserveAutoWrap = autoWrapEnabled
        val preserveBracketedPaste = bracketedPasteMode
        val preserveApplicationCursorKeys = applicationCursorKeys
        val preserveInsertMode = insertMode
        reset()
        useAltScreen = preserveAltScreen
        autoWrapEnabled = preserveAutoWrap
        bracketedPasteMode = preserveBracketedPaste
        applicationCursorKeys = preserveApplicationCursorKeys
        insertMode = preserveInsertMode
        markFullDirty()
    }

    fun getActiveScreen(): Array<LongArray> = if (useAltScreen) altScreen else screen

    fun putChar(c: Char) = putCodePoint(c.code)

    fun putCodePoint(codePoint: Int) = lock.withLock {
        val active = getActiveScreen()

        if (autoWrapPending && autoWrapEnabled) {
            cursorX = 0
            if (cursorY == scrollBottom) {
                scrollUp(1)
            } else if (cursorY < rows - 1) {
                cursorY++
            }
            autoWrapPending = false
        }

        if (insertMode) insertChars(1)
        active[cursorY][cursorX] = TerminalCell.createFrom(codePoint, TerminalCell(currentAttrs)).encoded
        markDirty(cursorX, cursorY)

        if (cursorX < cols - 1) {
            cursorX++
        } else {
            autoWrapPending = true // Wait for next char to actually wrap
        }
    }

    fun carriageReturn() = lock.withLock {
        cursorX = 0
        autoWrapPending = false
    }

    fun lineFeed() = lock.withLock {
        if (cursorY == scrollBottom) {
            scrollUp(1)
        } else if (cursorY < rows - 1) {
            cursorY++
        }
        autoWrapPending = false
    }

    fun backspace() = lock.withLock {
        if (cursorX > 0) {
            cursorX--
            autoWrapPending = false
        }
    }

    fun tab() = lock.withLock {
        val nextTabStop = ((cursorX / 8) + 1) * 8
        cursorX = min(nextTabStop, cols - 1)
        autoWrapPending = false
    }

    fun moveCursorAbsolute(y: Int, x: Int) = lock.withLock {
        cursorX = min(max(x, 0), cols - 1)
        cursorY = min(max(y, 0), rows - 1)
        autoWrapPending = false
    }

    fun moveCursorRelative(dy: Int, dx: Int) = lock.withLock {
        moveCursorAbsolute(cursorY + dy, cursorX + dx)
    }

    fun setCursorVisible(visible: Boolean) = lock.withLock {
        cursorVisible = visible
        markFullDirty()
    }

    fun setAutoWrap(enabled: Boolean) = lock.withLock {
        autoWrapEnabled = enabled
    }

    fun setBracketedPaste(enabled: Boolean) = lock.withLock {
        bracketedPasteMode = enabled
    }

    fun setApplicationCursorKeys(enabled: Boolean) = lock.withLock {
        applicationCursorKeys = enabled
    }

    fun setInsertMode(enabled: Boolean) = lock.withLock {
        insertMode = enabled
    }

    fun eraseInLine(mode: Int) = lock.withLock {
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        when (mode) {
            0 -> { // Cursor to end
                for (x in cursorX until cols) active[cursorY][x] = blank
                markRowDirty(cursorY)
            }
            1 -> { // Start to cursor
                for (x in 0..cursorX) active[cursorY][x] = blank
                markRowDirty(cursorY)
            }
            2 -> { // Entire line
                for (x in 0 until cols) active[cursorY][x] = blank
                markRowDirty(cursorY)
            }
        }
        autoWrapPending = false
    }

    fun eraseInDisplay(mode: Int) = lock.withLock {
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        when (mode) {
            0 -> { // Cursor to end of screen
                eraseInLine(0)
                for (y in cursorY + 1 until rows) {
                    for (x in 0 until cols) active[y][x] = blank
                    markRowDirty(y)
                }
            }
            1 -> { // Start of screen to cursor
                for (y in 0 until cursorY) {
                    for (x in 0 until cols) active[y][x] = blank
                    markRowDirty(y)
                }
                eraseInLine(1)
            }
            2 -> { // Entire screen
                for (y in 0 until rows) {
                    for (x in 0 until cols) active[y][x] = blank
                }
                markFullDirty()
            }
            3 -> { // xterm: erase saved lines (scrollback), not the visible screen
                if (!useAltScreen) {
                    scrollback.clear()
                    scrollbackVersion++
                    markFullDirty()
                }
            }
        }
    }

    fun setScrollRegion(top: Int, bottom: Int) = lock.withLock {
        scrollTop = min(max(top, 0), rows - 1)
        scrollBottom = min(max(bottom, scrollTop), rows - 1)
        moveCursorAbsolute(0, 0)
    }

    fun scrollUp(n: Int) = lock.withLock {
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        val count = min(n, scrollBottom - scrollTop + 1)

        // Save to scrollback if rolling top of screen (and not alt screen)
        if (!useAltScreen && scrollTop == 0) {
            for (i in 0 until count) {
                if (scrollback.size >= MAX_SCROLLBACK) scrollback.removeFirst()
                scrollback.addLast(active[i].copyOf())
            }
            scrollbackVersion++
        }

        // Shift lines up
        for (y in scrollTop..scrollBottom - count) {
            System.arraycopy(active[y + count], 0, active[y], 0, cols)
        }

        // Clear bottom lines
        for (y in scrollBottom - count + 1..scrollBottom) {
            for (x in 0 until cols) active[y][x] = blank
        }
        markFullDirty()
    }

    fun scrollDown(n: Int) = lock.withLock {
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        val count = min(n, scrollBottom - scrollTop + 1)

        // Shift lines down
        for (y in scrollBottom downTo scrollTop + count) {
            System.arraycopy(active[y - count], 0, active[y], 0, cols)
        }

        // Clear top lines
        for (y in scrollTop until scrollTop + count) {
            for (x in 0 until cols) active[y][x] = blank
        }
        markFullDirty()
    }

    fun reverseIndex() = lock.withLock {
        if (cursorY == scrollTop) {
            scrollDown(1)
        } else if (cursorY > 0) {
            cursorY--
        }
    }

    fun saveCursor() = lock.withLock {
        savedCursorX = cursorX
        savedCursorY = cursorY
        savedCurrentAttrs = currentAttrs
    }

    fun restoreCursor() = lock.withLock {
        cursorX = min(savedCursorX, cols - 1)
        cursorY = min(savedCursorY, rows - 1)
        currentAttrs = savedCurrentAttrs
        autoWrapPending = false
    }

    fun useAltScreen(
        use: Boolean,
        saveAndRestoreCursor: Boolean = false,
        clearOnEnter: Boolean = true
    ) = lock.withLock {
        if (useAltScreen != use) {
            if (use && saveAndRestoreCursor) {
                savedMainCursorX = cursorX
                savedMainCursorY = cursorY
                savedMainAttrs = currentAttrs
            }
            useAltScreen = use
            markFullDirty()
            if (use && clearOnEnter) {
                // Clear alt screen on enter
                val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
                for (y in 0 until rows) {
                    for (x in 0 until cols) altScreen[y][x] = blank
                }
                cursorX = 0
                cursorY = 0
                autoWrapPending = false
            } else if (!use && saveAndRestoreCursor) {
                cursorX = savedMainCursorX.coerceIn(0, cols - 1)
                cursorY = savedMainCursorY.coerceIn(0, rows - 1)
                currentAttrs = savedMainAttrs
                autoWrapPending = false
            }
        }
    }

    fun resize(newCols: Int, newRows: Int) = lock.withLock {
        require(newCols > 0 && newRows > 0) { "Terminal dimensions must be positive" }
        if (newCols == cols && newRows == rows) return@withLock

        val newScreen = Array(newRows) { LongArray(newCols) { TerminalCell.BLANK.encoded } }
        val newAltScreen = Array(newRows) { LongArray(newCols) { TerminalCell.BLANK.encoded } }

        val copyCols = min(cols, newCols)
        val copyRows = min(rows, newRows)

        for (y in 0 until copyRows) {
            System.arraycopy(screen[y], 0, newScreen[y], 0, copyCols)
            System.arraycopy(altScreen[y], 0, newAltScreen[y], 0, copyCols)
        }

        // Adjust cursor
        cursorX = min(cursorX, newCols - 1)
        cursorY = min(cursorY, newRows - 1)

        // Adjust scroll region if it covered full screen
        if (scrollTop == 0 && scrollBottom == rows - 1) {
            scrollBottom = newRows - 1
        } else {
            scrollTop = min(scrollTop, newRows - 1)
            scrollBottom = min(scrollBottom, newRows - 1)
        }

        cols = newCols
        rows = newRows
        screen = newScreen
        altScreen = newAltScreen
        markFullDirty()
    }

    fun insertChars(n: Int) = lock.withLock {
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        val count = min(n, cols - cursorX)
        for (x in cols - 1 downTo cursorX + count) {
            active[cursorY][x] = active[cursorY][x - count]
        }
        for (x in cursorX until cursorX + count) {
            active[cursorY][x] = blank
        }
        markRowDirty(cursorY)
    }

    fun deleteChars(n: Int) = lock.withLock {
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        val count = min(n, cols - cursorX)
        for (x in cursorX until cols - count) {
            active[cursorY][x] = active[cursorY][x + count]
        }
        for (x in cols - count until cols) {
            active[cursorY][x] = blank
        }
        markRowDirty(cursorY)
    }

    fun insertLines(n: Int) = lock.withLock {
        if (cursorY < scrollTop || cursorY > scrollBottom) return@withLock
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        val count = min(n, scrollBottom - cursorY + 1)

        for (y in scrollBottom downTo cursorY + count) {
            System.arraycopy(active[y - count], 0, active[y], 0, cols)
        }
        for (y in cursorY until cursorY + count) {
            for (x in 0 until cols) active[y][x] = blank
        }
        markFullDirty()
    }

    fun deleteLines(n: Int) = lock.withLock {
        if (cursorY < scrollTop || cursorY > scrollBottom) return@withLock
        val active = getActiveScreen()
        val blank = TerminalCell.createFrom(' ', TerminalCell(currentAttrs)).encoded
        val count = min(n, scrollBottom - cursorY + 1)

        for (y in cursorY..scrollBottom - count) {
            System.arraycopy(active[y + count], 0, active[y], 0, cols)
        }
        for (y in scrollBottom - count + 1..scrollBottom) {
            for (x in 0 until cols) active[y][x] = blank
        }
        markFullDirty()
    }

    // Returns a Snapshot object containing copies of arrays to safely render off-thread
    fun getSnapshot(): TerminalSnapshot = lock.withLock {
        val active = getActiveScreen()
        val screenCopy = Array(rows) { y -> active[y].copyOf() }
        val scrollbackCopy = if (!useAltScreen) {
            if (cachedScrollbackVersion != scrollbackVersion) {
                cachedScrollback = scrollback.toList()
                cachedScrollbackVersion = scrollbackVersion
            }
            cachedScrollback
        } else {
            emptyList()
        }

        val snap = TerminalSnapshot(
            cols = cols,
            rows = rows,
            cursorX = cursorX,
            cursorY = cursorY,
            cursorVisible = cursorVisible,
            bracketedPasteMode = bracketedPasteMode,
            screen = screenCopy,
            scrollback = scrollbackCopy,
            fullRedraw = fullRedrawRequired,
            dirtyMinX = dirtyMinX,
            dirtyMinY = dirtyMinY,
            dirtyMaxX = dirtyMaxX,
            dirtyMaxY = dirtyMaxY
        )

        // Reset dirty trackers
        dirtyMinX = cols
        dirtyMinY = rows
        dirtyMaxX = -1
        dirtyMaxY = -1
        fullRedrawRequired = false

        return snap
    }
}

data class TerminalSnapshot(
    val cols: Int,
    val rows: Int,
    val cursorX: Int,
    val cursorY: Int,
    val cursorVisible: Boolean,
    val bracketedPasteMode: Boolean,
    val screen: Array<LongArray>,
    val scrollback: List<LongArray>,
    val fullRedraw: Boolean,
    val dirtyMinX: Int,
    val dirtyMinY: Int,
    val dirtyMaxX: Int,
    val dirtyMaxY: Int
)
