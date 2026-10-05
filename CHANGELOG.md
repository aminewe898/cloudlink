# Changelog

All notable changes to the CloudLink project will be documented in this file.

## [4.1.0] - Operations Console UI

### Added
- Introduced a shared visual system with layered backgrounds, outlined panels, stronger spacing, and a compact primary navigation dock.
- Added a fleet overview with saved, favorite, and production node summaries plus richer server workspace cards.
- Added explicit terminal connection states, session identity, reconnect feedback, adjustable text size, local screen clearing, and live/scrollback indicators.
- Added multiline paste confirmation and negotiated bracketed-paste support for interactive terminal applications.

### Changed
- Removed direct Terminal and SFTP navigation entries that could open without a selected server; these tools now open from a specific server workspace.
- Improved server forms with password masking, reveal controls, folder selection, and required-field validation.
- Preserved negotiated terminal modes when clearing local history and made reconnect lifecycle handling safer.

### Hardened
- Serialized SSH connection lifecycle with stale-connect invalidation, explicit phases, stable failure messages, key validation, and known-host management.
- Bounded terminal input writes; corrected alternate-screen, erase-history, multi-mode, cursor-key, insert-mode, DSR/DA, and true-color approximation behavior.
- Added adaptive/safe-inset UI, lifecycle-aware state collection, hardware-key handling, secure key reveal, and timed private-key clipboard cleanup.
- Serialized SFTP operations, made editor writes temp-plus-rename, added safe conflicts/cancellation, and corrected foreground-transfer completion ordering.
- Made telemetry locale-independent and partial so one unavailable metric no longer discards the remaining sample.
- Added Room schema export, conditional external release signing, API-24 lint compatibility, release R8 rules, and regression tests.

## [4.0.1] - Stabilization and Security Corrections

### Fixed
- Made SSH connection creation idempotent and thread-safe, rejected changed host keys, and prevented feature ViewModels from disconnecting shared sessions.
- Replaced the destructive Room 1-to-2 migration with a compatibility-preserving table migration.
- Excluded encrypted credentials and device-specific host trust from Android backup and device transfer.
- Added biometric/device-credential availability checks and removed insecure authentication fallback behavior.
- Preserved supplementary Unicode code points in terminal cells, rendered additional text attributes, kept manual scrollback position, and eliminated deep copies of immutable scrollback rows.
- Added SFTP editor size and binary-file limits, safe remote-name validation, and batched deletion refreshes.
- Replaced template tests, removed unused AI/network dependencies, and updated CI to run lint and unit tests with supported GitHub actions.

*Note: Earlier development history has been consolidated into this document to accurately reflect the codebase's current production-hardened state.*

## [4.0.0] - Production Hardening & Rewrite Release

### Added
- **VT100 Terminal Engine:** Built a completely custom, native `Canvas`-based terminal emulator from scratch.
  - Implements a stateful streaming escape-sequence parser (`Vt100Parser.kt`).
  - Native rendering using `Typeface.MONOSPACE` locked at 60 FPS.
  - Features 10,000-line scrollback buffer and xterm 256-color support.
  - Virtual keyboard support handling Backspace/DEL reliably via `TextFieldValue` state tricking.
- **Hardware Encryption:** Integrated `EncryptedSharedPreferences` utilizing Android Keystore AES256-GCM for server credentials.
- **Network Utilities:** Added `ToolsScreen` encompassing Ping, Wake-on-LAN (WOL), and local RSA-2048 SSH Key Generation.

### Changed
- **Terminal Rendering Architecture:** Scrapped the previous `LazyColumn` + `AnnotatedString` regex-based UI in favor of a strictly optimized, bit-packed 64-bit `Long` grid (`TerminalBuffer.kt`), eliminating millions of object allocations per session.
- **Input Pipeline:** Refactored I/O reader/writer coroutines in `TerminalViewModel` to run strictly off the main thread, isolating UI render frames from network bursts.
- **Dependency Upgrades:** Migrated completely to Jetpack Compose Material 3 and updated Room database structures for reliability.

### Removed
- **Legacy Regex Parser:** Deleted the brittle `VirtualTerminal.kt` regex parser that crashed under heavy logging loads.
- **Dead Code:** Purged unused ViewModels, dummy repositories, and placeholder screens to stabilize the commercial build.

## [3.x.x and earlier] - Consolidated History
*Earlier versions involved rapid prototyping of the Dashboard, SFTP File Manager, Code Editor, and standard JSch SSH integration. This history has been consolidated following the architectural stabilization in v4.0.0.*
