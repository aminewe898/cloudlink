package com.cloudlink.app.ui.screens

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudlink.app.terminal.TerminalCell
import com.cloudlink.app.terminal.TerminalSnapshot
import com.cloudlink.app.ui.viewmodel.TerminalViewModel
import com.cloudlink.app.ui.viewmodel.TerminalConnectionStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.floor
import kotlin.math.max

// XTerm 256 Color Palette
private val XTERM_COLORS = Array(256) { Color.Black }.apply {
    // 0-7: Standard
    this[0] = Color(0xFF000000)
    this[1] = Color(0xFFCD0000)
    this[2] = Color(0xFF00CD00)
    this[3] = Color(0xFFCDCD00)
    this[4] = Color(0xFF0000EE)
    this[5] = Color(0xFFCD00CD)
    this[6] = Color(0xFF00CDCD)
    this[7] = Color(0xFFE5E5E5)

    // 8-15: Bright
    this[8] = Color(0xFF7F7F7F)
    this[9] = Color(0xFFFF0000)
    this[10] = Color(0xFF00FF00)
    this[11] = Color(0xFFFFFF00)
    this[12] = Color(0xFF5C5CFF)
    this[13] = Color(0xFFFF00FF)
    this[14] = Color(0xFF00FFFF)
    this[15] = Color(0xFFFFFFFF)

    // 16-231: Color cube (6x6x6)
    val colorValues = intArrayOf(0x00, 0x5F, 0x87, 0xAF, 0xD7, 0xFF)
    for (r in 0 until 6) {
        for (g in 0 until 6) {
            for (b in 0 until 6) {
                val index = 16 + (r * 36) + (g * 6) + b
                this[index] = Color(
                    red = colorValues[r],
                    green = colorValues[g],
                    blue = colorValues[b]
                )
            }
        }
    }

    // 232-255: Grayscale
    for (i in 0 until 24) {
        val level = 8 + (i * 10)
        this[232 + i] = Color(level, level, level)
    }
}

private val DEFAULT_FG = XTERM_COLORS[7]
private val DEFAULT_BG = XTERM_COLORS[0]
private val TERMINAL_TYPEFACES = arrayOf(
    Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL),
    Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
    Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
    Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
)

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    serverId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.terminalSnapshot.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val sessionLabel by viewModel.sessionLabel.collectAsStateWithLifecycle()
    val statusDetail by viewModel.statusDetail.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var ctrlPressed by remember { mutableStateOf(false) }
    var altPressed by remember { mutableStateOf(false) }
    var terminalFontSize by rememberSaveable { mutableIntStateOf(14) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingPaste by remember { mutableStateOf<String?>(null) }
    var followingLiveOutput by remember { mutableStateOf(true) }
    var scrollToLiveSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(serverId) {
        if (serverId != -1) {
            viewModel.connect(serverId)
        }
    }

    // Auto request focus
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> viewModel.sendCursorKey('A')
                    Key.DirectionDown -> viewModel.sendCursorKey('B')
                    Key.DirectionRight -> viewModel.sendCursorKey('C')
                    Key.DirectionLeft -> viewModel.sendCursorKey('D')
                    Key.Enter, Key.NumPadEnter -> viewModel.sendString("\r")
                    Key.Backspace -> viewModel.sendBytes(byteArrayOf(0x7F))
                    Key.Delete -> viewModel.sendString("\u001B[3~")
                    Key.Tab -> viewModel.sendString("\t")
                    Key.Escape -> viewModel.sendString("\u001B")
                    Key.PageUp -> viewModel.sendString("\u001B[5~")
                    Key.PageDown -> viewModel.sendString("\u001B[6~")
                    Key.MoveHome -> viewModel.sendString("\u001B[H")
                    Key.MoveEnd -> viewModel.sendString("\u001B[F")
                    else -> {
                        val unicode = event.nativeKeyEvent.unicodeChar
                        when {
                            event.isCtrlPressed && unicode > 0 -> {
                                val upper = unicode.toChar().uppercaseChar()
                                if (upper in '@'..'_') {
                                    viewModel.sendBytes(byteArrayOf((upper.code - 64).toByte()))
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent false
                            }
                            event.isAltPressed && unicode > 0 -> {
                                viewModel.sendString("\u001B${String(Character.toChars(unicode))}")
                                return@onPreviewKeyEvent true
                            }
                            else -> return@onPreviewKeyEvent false
                        }
                    }
                }
                true
            }
            .background(Color(0xFF070A0F))
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp, top = 5.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = terminalStatusColor(connectionStatus).copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = terminalStatusColor(connectionStatus)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sessionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isConnected && serverId != -1) {
                        IconButton(onClick = { viewModel.connect(serverId) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Terminal options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Clear local screen") },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.clearTerminal()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Smaller text") },
                                leadingIcon = { Icon(Icons.Default.TextDecrease, contentDescription = null) },
                                enabled = terminalFontSize > 10,
                                onClick = { terminalFontSize = (terminalFontSize - 1).coerceAtLeast(10) }
                            )
                            DropdownMenuItem(
                                text = { Text("Larger text") },
                                leadingIcon = { Icon(Icons.Default.TextIncrease, contentDescription = null) },
                                enabled = terminalFontSize < 22,
                                onClick = { terminalFontSize = (terminalFontSize + 1).coerceAtMost(22) }
                            )
                            if (isConnected) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Disconnect", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.disconnect()
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(terminalStatusColor(connectionStatus).copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(terminalStatusColor(connectionStatus), RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = terminalStatusLabel(connectionStatus),
                        color = terminalStatusColor(connectionStatus),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (followingLiveOutput) "LIVE" else "SCROLLBACK",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${terminalFontSize}sp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Terminal Rendering Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Hidden TextField to capture input
            var textFieldValue by remember { mutableStateOf(TextFieldValue(" ", selection = TextRange(1))) }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    val newText = newValue.text

                    if (newText.length < oldText.length) {
                        // Backspace pressed! Send ASCII DEL (0x7F)
                        viewModel.sendBytes(byteArrayOf(0x7F))
                    } else if (newText.length > oldText.length) {
                        val typed = newText.substring(oldText.length)
                        if (ctrlPressed) {
                            typed.forEach { c ->
                                val upper = c.uppercaseChar()
                                if (upper in '@'..'_') {
                                    val code = upper.code - 64
                                    viewModel.sendBytes(byteArrayOf(code.toByte()))
                                }
                            }
                            ctrlPressed = false
                        } else {
                            val input = typed.replace("\n", "\r")
                            viewModel.sendString(if (altPressed) "\u001B$input" else input)
                            altPressed = false
                        }
                    }
                    // Reset to dummy state to keep capturing backspaces
                    textFieldValue = TextFieldValue(" ", selection = TextRange(1))
                },
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text
                )
            )

            // The Canvas Renderer
            TerminalCanvasRenderer(
                snapshot = snapshot,
                fontSizeSp = terminalFontSize,
                scrollToBottomSignal = scrollToLiveSignal,
                onFollowingLiveChange = { followingLiveOutput = it },
                onSizeChanged = { cols, rows, widthPx, heightPx ->
                    viewModel.updateTerminalSize(cols, rows, widthPx, heightPx)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        )
                    }
            )
            if (!isConnected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (connectionStatus) {
                                TerminalConnectionStatus.ERROR -> Icons.Default.ErrorOutline
                                TerminalConnectionStatus.CONNECTING,
                                TerminalConnectionStatus.RECONNECTING -> Icons.Default.Sync
                                else -> Icons.Default.Terminal
                            },
                            contentDescription = null,
                            tint = terminalStatusColor(connectionStatus),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = terminalStatusLabel(connectionStatus),
                            style = MaterialTheme.typography.titleMedium,
                            color = terminalStatusColor(connectionStatus)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (connectionStatus == TerminalConnectionStatus.ERROR ||
                            connectionStatus == TerminalConnectionStatus.DISCONNECTED
                        ) {
                            Spacer(Modifier.height(14.dp))
                            FilledTonalButton(onClick = { viewModel.connect(serverId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reconnect")
                            }
                        }
                    }
                }
            }
            if (!followingLiveOutput) {
                SmallFloatingActionButton(
                    onClick = { scrollToLiveSignal++ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.South, contentDescription = "Jump to live output")
                }
            }
        }

        if (isConnected) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    // CTRL Toggle
                    Surface(
                        onClick = { ctrlPressed = !ctrlPressed },
                        color = if (ctrlPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("CTRL", fontWeight = FontWeight.Bold, color = if (ctrlPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Surface(
                        onClick = { altPressed = !altPressed },
                        color = if (altPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("ALT", fontWeight = FontWeight.Bold, color = if (altPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { QuickKey("ESC") { viewModel.sendString("\u001B") } }
                item { QuickKey("TAB") { viewModel.sendString("\t") } }
                item { QuickKey("ENTER") { viewModel.sendString("\r") } }
                item { QuickKey("↑") { viewModel.sendCursorKey('A') } }
                item { QuickKey("↓") { viewModel.sendCursorKey('B') } }
                item { QuickKey("←") { viewModel.sendCursorKey('D') } }
                item { QuickKey("→") { viewModel.sendCursorKey('C') } }
                item { QuickKey("PgUp") { viewModel.sendString("\u001B[5~") } }
                item { QuickKey("PgDn") { viewModel.sendString("\u001B[6~") } }
                item { QuickKey("Home") { viewModel.sendString("\u001B[H") } }
                item { QuickKey("End") { viewModel.sendString("\u001B[F") } }
                item { QuickKey("F1") { viewModel.sendString("\u001BOP") } }
                item { QuickKey("F2") { viewModel.sendString("\u001BOQ") } }
                item { QuickKey("F3") { viewModel.sendString("\u001BOR") } }
                item { QuickKey("F4") { viewModel.sendString("\u001BOS") } }
                item {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { clipboardText ->
                            if ('\n' in clipboardText || clipboardText.length > 200) {
                                pendingPaste = clipboardText
                            } else {
                                viewModel.pasteText(clipboardText)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, "Paste", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    pendingPaste?.let { text ->
        val lineCount = text.lineSequence().count()
        AlertDialog(
            onDismissRequest = { pendingPaste = null },
            icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
            title = { Text("Paste into remote shell?") },
            text = {
                Text("This will send $lineCount lines (${text.length} characters) to the server. Review multiline commands before continuing.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.pasteText(text)
                    pendingPaste = null
                }) { Text("Paste") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPaste = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun terminalStatusColor(status: TerminalConnectionStatus): Color = when (status) {
    TerminalConnectionStatus.CONNECTED -> Color(0xFF34D399)
    TerminalConnectionStatus.CONNECTING,
    TerminalConnectionStatus.RECONNECTING -> Color(0xFFFBBF24)
    TerminalConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
    TerminalConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun terminalStatusLabel(status: TerminalConnectionStatus): String = when (status) {
    TerminalConnectionStatus.CONNECTED -> "SECURE SESSION ACTIVE"
    TerminalConnectionStatus.CONNECTING -> "CONNECTING"
    TerminalConnectionStatus.RECONNECTING -> "RECONNECTING"
    TerminalConnectionStatus.ERROR -> "CONNECTION ERROR"
    TerminalConnectionStatus.DISCONNECTED -> "DISCONNECTED"
}

@Composable
fun QuickKey(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TerminalCanvasRenderer(
    snapshot: TerminalSnapshot?,
    onSizeChanged: (Int, Int, Int, Int) -> Unit,
    fontSizeSp: Int = 14,
    scrollToBottomSignal: Int = 0,
    onFollowingLiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // We use a mutable state for scroll offset
    var scrollOffset by remember { mutableStateOf(0f) }
    var followLiveOutput by remember { mutableStateOf(true) }
    BoxWithConstraints(modifier = modifier) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        // Font setup using native Android Paint for maximum performance
        val textPaint = remember(density, fontSizeSp) {
            android.graphics.Paint().apply {
                typeface = Typeface.MONOSPACE
                textSize = with(density) { fontSizeSp.sp.toPx() }
                isAntiAlias = true
            }
        }

        // Measure character dimensions
        val fm = textPaint.fontMetrics
        val charHeight = fm.descent - fm.ascent
        val charWidth = textPaint.measureText("W") // Monospace, so 'W' is same as 'i'

        // Calculate grid size
        val cols = max(1, floor(viewWidth / charWidth).toInt())
        val rows = max(1, floor(viewHeight / charHeight).toInt())

        LaunchedEffect(cols, rows, viewWidth, viewHeight) {
            onSizeChanged(cols, rows, viewWidth.toInt(), viewHeight.toInt())
        }

        LaunchedEffect(followLiveOutput) {
            onFollowingLiveChange(followLiveOutput)
        }

        LaunchedEffect(scrollToBottomSignal) {
            if (scrollToBottomSignal > 0) {
                scrollOffset = 0f
                followLiveOutput = true
            }
        }

        // Render phase
        if (snapshot != null) {
            // Auto scroll to bottom when new content arrives
            LaunchedEffect(snapshot.scrollback.size) {
                if (followLiveOutput) scrollOffset = 0f
            }

            val maxScrollOffset = max(0f, snapshot.scrollback.size * charHeight)
            LaunchedEffect(maxScrollOffset) {
                scrollOffset = scrollOffset.coerceIn(0f, maxScrollOffset)
                if (scrollOffset < charHeight / 2f) followLiveOutput = true
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Interactive SSH terminal. Terminal text is drawn on a canvas."
                    }
                    .pointerInput(maxScrollOffset) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            scrollOffset = (scrollOffset - dragAmount.y).coerceIn(0f, maxScrollOffset)
                            followLiveOutput = scrollOffset < charHeight / 2f
                        }
                    }
            ) {
                // Background
                drawRect(color = DEFAULT_BG)

                // How many scrollback lines are visible?
                val visibleScrollbackLines = (scrollOffset / charHeight).toInt()
                val yOffsetRemainder = scrollOffset % charHeight

                // Draw lines from top to bottom
                val textBuilder = StringBuilder(cols * 2)
                for (y in 0 until rows) {
                    val drawY = (y * charHeight) - yOffsetRemainder
                    if (drawY > viewHeight) break

                    val absoluteRow = snapshot.scrollback.size - visibleScrollbackLines + y
                    val isScrollbackLine = absoluteRow < snapshot.scrollback.size

                    val line = if (isScrollbackLine) {
                        if (absoluteRow >= 0) snapshot.scrollback[absoluteRow] else null
                    } else {
                        val screenY = absoluteRow - snapshot.scrollback.size
                        if (screenY in 0 until snapshot.rows) snapshot.screen[screenY] else null
                    }

                    if (line == null) continue

                    // Batch draw characters with same style
                    var currentFg = -1
                    var currentBg = -1
                    var isBold = false
                    var isItalic = false
                    var isUnderline = false
                    var isDim = false
                    textBuilder.clear()
                    var startX = 0
                    var runCellCount = 0

                    for (x in 0 until cols) {
                        if (x >= line.size) break

                        val cellLong = line[x]
                        val cell = TerminalCell(cellLong)

                        if (cell.isHidden) {
                            // If we have accumulated text, draw it before skipping
                            if (textBuilder.isNotEmpty()) {
                                drawTextRun(this, textBuilder.toString(), runCellCount, startX, drawY, charWidth, charHeight, currentFg, currentBg, isBold, isItalic, isUnderline, isDim, textPaint)
                                textBuilder.clear()
                                runCellCount = 0
                            }
                            startX = x + 1
                            continue
                        }

                        var cellFg = if (cell.isFgDefault) -1 else cell.fgIndex
                        var cellBg = if (cell.isBgDefault) -1 else cell.bgIndex
                        val cellBold = cell.isBold
                        val cellItalic = cell.isItalic
                        val cellUnderline = cell.isUnderline
                        val cellDim = cell.isDim

                        if (cell.isReverse) {
                            val temp = cellFg
                            cellFg = if (cellBg == -1) 0 else cellBg
                            cellBg = if (temp == -1) 7 else temp
                        }

                        // Cursor logic
                        val isCursor = (!isScrollbackLine && snapshot.cursorVisible && x == snapshot.cursorX && (absoluteRow - snapshot.scrollback.size) == snapshot.cursorY)
                        if (isCursor) {
                            // Cursor block is usually drawn by reversing colors
                            cellBg = 7 // White block
                            cellFg = 0 // Black text
                        }

                        if (x == 0 || cellFg != currentFg || cellBg != currentBg ||
                            cellBold != isBold || cellItalic != isItalic ||
                            cellUnderline != isUnderline || cellDim != isDim
                        ) {
                            if (textBuilder.isNotEmpty()) {
                                drawTextRun(this, textBuilder.toString(), runCellCount, startX, drawY, charWidth, charHeight, currentFg, currentBg, isBold, isItalic, isUnderline, isDim, textPaint)
                                textBuilder.clear()
                                runCellCount = 0
                            }
                            currentFg = cellFg
                            currentBg = cellBg
                            isBold = cellBold
                            isItalic = cellItalic
                            isUnderline = cellUnderline
                            isDim = cellDim
                            startX = x
                        }

                        textBuilder.appendCodePoint(cell.codePoint)
                        runCellCount++
                    }

                    if (textBuilder.isNotEmpty()) {
                        drawTextRun(this, textBuilder.toString(), runCellCount, startX, drawY, charWidth, charHeight, currentFg, currentBg, isBold, isItalic, isUnderline, isDim, textPaint)
                    }
                }
            }
        }
    }
}

private fun drawTextRun(
    scope: DrawScope,
    text: String,
    cellCount: Int,
    colStart: Int,
    yPos: Float,
    charWidth: Float,
    charHeight: Float,
    fgIndex: Int,
    bgIndex: Int,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    isDim: Boolean,
    paint: android.graphics.Paint
) {
    // Draw background block if not default
    if (bgIndex != -1) {
        val bgColor = XTERM_COLORS[bgIndex.coerceIn(0, 255)]
        scope.drawRect(
            color = bgColor,
            topLeft = Offset(colStart * charWidth, yPos),
            size = androidx.compose.ui.geometry.Size(charWidth * cellCount, charHeight)
        )
    }

    // Draw text
    // Only blank space optimization if fg isn't changed/needed
    if (text.isNotBlank() || bgIndex != -1) {
        val fgColor = if (fgIndex != -1) XTERM_COLORS[fgIndex.coerceIn(0, 255)] else DEFAULT_FG

        val alphaMultiplier = if (isDim) 0.55f else 1f
        paint.color = android.graphics.Color.argb(
            (fgColor.alpha * alphaMultiplier * 255).toInt(),
            (fgColor.red * 255).toInt(),
            (fgColor.green * 255).toInt(),
            (fgColor.blue * 255).toInt()
        )

        val typefaceStyle = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val typefaceIndex = when (typefaceStyle) {
            Typeface.BOLD -> 1
            Typeface.ITALIC -> 2
            Typeface.BOLD_ITALIC -> 3
            else -> 0
        }
        paint.typeface = TERMINAL_TYPEFACES[typefaceIndex]
        paint.isUnderlineText = isUnderline

        scope.drawContext.canvas.nativeCanvas.drawText(
            text,
            colStart * charWidth,
            yPos - paint.ascent(),
            paint
        )
    }
}
