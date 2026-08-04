# CloudLink Feature Reference

This document provides an exhaustive, code-verified reference of every major subsystem currently implemented in CloudLink.

## 1. SSH Subsystem
**Component:** `SshConnectionManager`
*   **Protocol Implementation:** Built on top of the JSch library.
*   **Authentication:** Supports standard Password authentication and RSA Private Key authentication.
*   **Host Verification:** Implements "Trust on first use" (TOFU); users confirm the first host-key prompt, while changed or mismatched keys are rejected.
*   **Session Management:** Maintains one synchronized, reusable session per server and safely executes commands in `Dispatchers.IO` coroutines.

## 2. Terminal Engine
**Components:** `TerminalBuffer`, `TerminalScreen`, `Vt100Parser`, `TerminalCell`
*   **Canvas Rendering:** Bypasses Android's standard text UI (like `LazyColumn`) for a native `Canvas` implementation painting `Typeface.MONOSPACE`.
*   **State Machine Parser:** Implements a stateful VT100 parser that handles ANSI escapes, CSI sequences, supplementary Unicode code points, and bracketed paste without regex overhead.
*   **Compact Cells:** Uses bit-packed 64-bit `Long` values (`TerminalCell`) to represent character state without per-cell objects.
*   **Render Scheduling:** Coalesces terminal snapshots and caps publication near 60 FPS while keeping I/O work off the main thread. No formal performance benchmark is published yet.
*   **Input Pipeline:** Uses a hidden `BasicTextField` with a dummy character state to reliably capture virtual keyboard strokes (including Backspace) across all Android IMEs (Gboard, SwiftKey, etc.).
*   **Color Palette:** Supports the xterm 256-color palette and approximates true-color SGR input to the nearest 256-color entry.
*   **Scrollback:** Supports up to 10,000 lines of scrollback memory.
*   **Quick Keys:** A horizontal action bar providing instant access to CTRL, ESC, TAB, Arrow Keys, PgUp/PgDn, Home/End, and Paste.

## 3. SFTP & File Management
**Components:** `SftpTransferService`, `FileManagerScreen`, `SftpViewModel`
*   **File Browsing:** Live directory listing and navigation over active SSH sessions via the SFTP channel.
*   **Operations:** Supports creating directories, renaming files, deleting items, and transferring files.
*   **Code Editor:** Opens text-like, non-symlink files up to 2 MiB and saves through a temporary sibling plus rename where supported. It is not collaborative or real-time editing.

## 4. Network Utilities (Tools)
**Components:** `ToolsScreen`, `ToolsViewModel`
*   **Ping:** Reachability testing utility for raw IP addresses or hostnames.
*   **Wake on LAN:** Broadcasts a magic packet to a specified MAC address (supports custom broadcast IPs) to wake dormant hardware.
*   **SSH Key Generator:** Generates a 4096-bit RSA key pair locally. The public key and fingerprint are shown; private-key reveal is explicit, protected from screenshots, and never saved automatically.

## 5. Security & App Lock
**Components:** `CredentialManager`, `LockScreen`
*   **Encrypted Storage:** Passwords and private keys are encrypted using AES256-GCM with a master key held by Android Keystore. Hardware backing is device-dependent.
*   **App Lock:** An integrated lock screen that restricts access to the dashboard using Android's native `BiometricPrompt` (Fingerprint, Face Unlock, or Device Credential).
*   *(For deeper security details, refer to `SECURITY.md`)*

## 6. Dashboard & Telemetry
**Components:** `DashboardScreen`, `DashboardViewModel`, `ServerListViewModel`
*   **Server Management:** Add, edit, and delete server profiles.
*   **Connection Logs:** Tracks connection events and errors via Room database (`ConnectionLogDao`).
*   **Portable Telemetry:** Collects independent best-effort Linux metrics with locale-independent markers and displays unavailable metrics honestly.

## 7. Themes & Customization
**Components:** `ThemeManager`, `ThemeSelector`
*   **Material 3:** Implements full Material Design 3 guidelines.
*   **Dynamic Theming:** Supports light and dark modes with customizable color palettes.

## Compatibility boundary

The terminal is a practical VT/ANSI subset, not a complete xterm implementation. Wide/combining/emoji grapheme layout, durable transfer resume, concurrent terminal tabs, encrypted private keys, and several advanced SSH features are not implemented. See [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md).
