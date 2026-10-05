# CloudLink Project Audit

Audit date: 2026-08-03<br>
Scope: the complete single-module Android repository as supplied, excluding generated build output<br>
Method: static inspection of all handwritten Kotlin, Gradle configuration, manifest/resources, backup rules, CI, tests, and project documentation. Build and test results are intentionally recorded separately in `CODEX_BASELINE_RESULTS.md` after this audit, in accordance with the requested phase gate.

## 1. Architecture overview

CloudLink is a single `:app` Android application using Compose, Hilt, Room, DataStore, coroutines, AndroidX Security, BiometricPrompt, and the maintained mwiede JSch fork. The implementation is compact (roughly 7,500 handwritten Kotlin lines) and is organized into data, domain-repository, terminal, and UI packages.

The application starts in `MainActivity`, applies an edge-to-edge Compose theme, observes the screen-protection preference, and hosts all navigation. The mandatory first route is the biometric/device-credential lock screen. Primary navigation exposes Servers, Sessions, and Tools. A server opens a detail/telemetry workspace, which can then open the terminal or SFTP browser.

The state flow is:

1. Room stores non-secret server profiles, snippets, and connection logs.
2. `CredentialManager` stores passwords/private keys in `EncryptedSharedPreferences`, keyed by Room server ID.
3. `SshConnectionManager` owns a process-wide map of JSch `Session` objects and a process-wide JSch known-hosts file.
4. Feature ViewModels call the manager directly for terminal, SFTP, telemetry, and session actions.
5. Compose screens collect ViewModel `StateFlow`s and render feature-local UI.

This is MVVM-shaped, but networking policy, lifecycle policy, error mapping, and session state are not represented by dedicated domain abstractions. Several ViewModels therefore mix orchestration, parsing, transport lifecycle, and presentation state.

### Application entry points and navigation

- `CloudLinkApplication`: Hilt application entry point.
- `MainActivity`: only exported component and launcher activity; owns theme, `FLAG_SECURE`, navigation, inactivity relock, and the global host-key dialog.
- `SftpTransferService`: non-exported data-sync foreground service used for uploads/downloads.
- `CloudLinkApp`: Compose `NavHost`, primary navigation, and host-key confirmation UI.

Terminal and SFTP ViewModels are explicitly scoped to the activity rather than individual navigation entries. This keeps one terminal/SFTP state holder alive across navigation, but it also makes server switching and cleanup implicit and prevents multiple independent terminal workspaces.

### Connection and terminal data flow

`TerminalScreen` calls `TerminalViewModel.connect(serverId)`. The ViewModel loads the profile, asks `SshConnectionManager` to create/reuse a JSch session, opens a `ChannelShell`, negotiates an xterm-256color PTY, and launches an IO reader. `InputStreamReader` decodes UTF-8 chunks, `Vt100Parser` mutates a locked `TerminalBuffer`, and a conflated render signal publishes copied `TerminalSnapshot`s. A Canvas renderer converts packed `TerminalCell` values to Android `Paint` runs. Keyboard/quick-key input is queued to a separate IO writer.

### SFTP and transfer flow

`SftpViewModel` keeps one SFTP channel for interactive directory and editor operations. The file manager uses the Storage Access Framework to obtain content URIs, then starts `SftpTransferService`. The service reconnects/reuses the shared SSH session, opens a separate SFTP channel, serializes all transfers behind one mutex, and streams between JSch and the content resolver while updating one foreground notification.

### Telemetry flow

`DashboardViewModel` connects through the shared SSH manager, runs shell command bundles through short-lived exec channels, parses delimiter-separated output, then polls a single all-or-nothing statistics command every three seconds while its navigation-scoped ViewModel remains alive.

### Credential and host-trust flow

Secrets are excluded from Room and stored with `EncryptedSharedPreferences` using an Android Keystore-backed master key. The `known_hosts` file lives in internal storage. JSch uses `StrictHostKeyChecking=ask`; its blocking callback bridges to a singleton coroutine coordinator and a Compose dialog. First-use acceptance updates known hosts through JSch. Changed keys are rejected by recognizing mismatch text in JSch's prompt.

## 2. Important modules and responsibilities

| Area | Main files | Current responsibility |
|---|---|---|
| App shell | `MainActivity.kt`, `Navigation.kt` | Edge-to-edge setup, relock, navigation, host-key prompt |
| Persistence | `AppDatabase.kt`, DAOs, entities | Server profiles, snippets, connection history, migration 1→2 |
| Credentials | `CredentialManager.kt` | Keystore-backed encrypted password/key storage |
| SSH | `SshConnectionManager.kt`, `HostKeyPromptCoordinator.kt` | Session map, authentication, known hosts, exec/shell/SFTP channels |
| Transfers | `SftpTransferService.kt` | Foreground upload/download streaming and notification |
| Terminal core | `TerminalCell.kt`, `TerminalBuffer.kt`, `Vt100Parser.kt` | Packed cells, screen/scrollback state, subset of VT/ANSI parsing |
| Terminal UI | `TerminalViewModel.kt`, `TerminalScreen.kt` | Shell lifecycle, IO queues, PTY resize, keyboard capture, Canvas rendering |
| SFTP UI | `SftpViewModel.kt`, `FileManagerScreen.kt`, `CodeEditorScreen.kt` | Directory operations, remote editor, SAF transfer launch |
| Telemetry | `DashboardViewModel.kt`, `ServerTelemetryParser.kt`, `ServerDetailScreen.kt` | Remote command bundles, parsing, polling, charts/system details |
| Server management | `ServerListViewModel.kt`, `ServersScreen.kt`, `ServerFormDialog.kt` | CRUD, credential association, folders/search/favorites |
| Utilities | `ToolsViewModel.kt`, `ToolsScreen.kt` | Ping process, Wake-on-LAN, RSA generation |
| Settings/security UI | `ThemeManager.kt`, `SettingsViewModel.kt`, `LockScreen.kt` | Theme/privacy preferences, history export, app authentication |

## 3. Current strengths

- Secrets are separated from the Room database and stored with authenticated encryption backed by Android Keystore key material.
- Credential preferences and known-host data are excluded from both cloud backup and device-to-device transfer rules.
- The only externally reachable component is the launcher activity; the transfer service is explicitly non-exported.
- Host verification is enabled rather than bypassed, first use requires user interaction, and obvious changed-key prompts fail closed.
- SSH, SFTP, terminal parsing, key generation, and process waiting are dispatched away from the main thread.
- SSH exec channels and feature SFTP/shell channels generally disconnect in `finally`/cleanup paths.
- The SSH manager attempts one reusable session per server and uses per-server connection mutexes for duplicate connect calls.
- Terminal scrollback is bounded at 10,000 rows and cells use a compact packed representation.
- Supplementary Unicode code points survive UTF-16 chunk boundaries and fit in packed cells.
- Terminal snapshots isolate the renderer from concurrent buffer mutation; render requests are conflated.
- SFTP editor reads are bounded to 2 MiB and reject NUL-containing binary content.
- Remote entry names reject path separators, NUL, `.` and `..`.
- SAF is used for user-selected upload/download locations; no broad storage permission is requested.
- Server/log Room relationships cascade history removal when a server is deleted.
- UI already contains useful empty states, content descriptions for major actions, a 48 dp dimension token, adaptive server grids, and narrow/wide telemetry chart layouts.
- CI runs lint, debug unit tests, and a debug APK build.

## 4. Critical defects

### C1. Disconnect can race an in-flight connect and resurrect a session

`SshConnectionManager.disconnect()` does not acquire the per-server connection mutex. It removes the current map entry and even removes the mutex while `connect()` may still be negotiating. The in-flight connect can subsequently insert a live session after the user requested disconnect. This affects explicit disconnect, profile deletion, and terminal server switching.

### C2. Terminal reconnect/shell startup is not serialized

`TerminalViewModel.startShell()` launches an untracked coroutine; multiple EOF/error paths can schedule multiple reconnect coroutines. A stale shell startup or delayed reconnect can update state after a newer connect. `performDisconnect()` also clears shared fields without a generation/token check. Duplicate channels, misleading states, and disconnecting the wrong logical attempt are possible.

### C3. Terminal write buffering is unbounded and failures are hidden

The terminal uses `Channel.UNLIMITED` for outgoing byte arrays. If the network stalls while a user pastes/types, memory can grow without bound. Writer exceptions are swallowed and the UI can remain connected while input is being discarded.

### C4. Editing authentication type can destroy the only credential

The edit form permits Save with an empty credential even after changing password ↔ key. `ServerListViewModel.updateServer()` deletes old credentials when auth type changes, then stores nothing for an empty field, leaving an unusable profile. Profile and credential writes are also not transactional or error-reported.

### C5. Generated private keys are exposed without a reveal gate

The key tool renders the complete private key immediately, includes it in a generic “copy output” action, and allows screen protection to have been disabled globally. There is no explicit reveal confirmation, separate public/private action, fingerprint presentation, or clipboard clearing. This violates the requested private-key handling requirements.

## 5. High-priority defects

### SSH/session reliability

- Session state is inferred from a set of IDs plus feature-local booleans; authenticating, verifying, disconnecting, and failure causes are not represented centrally.
- `activeSessionIds` is refreshed only during manager calls. A remotely closed idle session can remain shown as active.
- Channel-opening helpers catch every exception and return `null`, discarding actionable failure causes.
- Error text is passed through directly from exceptions; there is no stable mapping for DNS, timeout, authentication, key format, host mismatch, permission, or network loss.
- JSch identity mutation occurs on a singleton JSch instance during per-server parallel connects without an explicit global identity lock.
- Exec output is read fully into unbounded strings. A noisy or malicious command can consume large memory.
- A host-key dialog can remain visible over the lock route after inactivity relock, exposing host details before re-authentication.
- Changed-host rejection depends on matching English prompt substrings rather than an explicit known-host decision model.
- There is no UI to inspect, fingerprint, or remove known hosts.

### Terminal correctness

- Alternate-screen mode 1049 does not save/restore cursor and attributes; 47, 1047, and 1049 are treated identically.
- Origin mode, insert mode, application cursor-key mode, character sets, tab-stop control, DCS, and device status reports are absent.
- Private mode processing considers only the first parameter in a multi-mode CSI sequence.
- True-color SGR (`38;2`/`48;2`) is not supported although documentation implies broad color compatibility.
- Combining marks, zero-width characters, East Asian wide characters, emoji grapheme clusters, variation selectors, and ZWJ sequences have no cell-width/grapheme model. Wide glyphs therefore overwrite/overlap following cells.
- Invalid UTF-8 is silently replaced by `InputStreamReader`; decoder policy and limitations are not tested/documented.
- `ED 3` is treated like `ED 2` and does not implement xterm scrollback erasure semantics.
- Alternate-screen entry always clears the alternate buffer and does not distinguish DEC modes.
- The Canvas ignores the buffer's dirty bounds and redraws every visible cell on every snapshot.
- Rendering allocates a `StringBuilder`, multiple `String`s, `TerminalCell.text` strings, and sometimes typefaces per row/run per frame.
- The hidden input field requests ASCII input, has fragile length-difference handling for IME composition/replacement, and has no hardware-key modifier pipeline.
- Cursor blink, bell feedback, selection, copy, search, and configurable scrollback remain unimplemented.

### SFTP/transfers

- Directory loads and mutations launch independent jobs without cancellation or a mutex; slower old requests can overwrite newer navigation state.
- `loadDirectory()` is called by launching another coroutine from inside mutation coroutines, so loading/error ordering is not deterministic.
- Remote text saves overwrite the target directly. A disconnect can leave a truncated file; there is no temp-upload + rename strategy.
- Download completion is announced inside the output-stream `use` block, before the stream has closed. A null content stream can silently end without success or failure.
- Upload conflicts overwrite by default; download interruptions leave partial content at the user-selected URI.
- Transfers have no user-visible cancellation action, retry, conflict confirmation, queue model, per-transfer identity, or process restoration.
- All transfers share one mutex and one notification ID, preventing independent progress and making a queue invisible.
- Invalid service intents do not explicitly stop the service.
- Notification permission is declared but no runtime request/denial UX is implemented for Android 13+.
- Symlink identity is discarded in `RemoteFile`; navigation and destructive operations cannot clearly distinguish links.
- Large directory listing is materialized and sorted in one operation with no paging/streaming.

### Telemetry robustness

- Polling relies on GNU-like `top`, `free`, `uptime -p`, `df`, `awk`, and `/proc/loadavg` output. BusyBox, Alpine, OpenWrt, localized output, restricted accounts, containers, and nonstandard `top` formats can fail.
- One missing or unparsable metric fails the entire telemetry monitor rather than marking that metric unavailable.
- CPU parsing from `top` is not portable and is locale-sensitive.
- The value labeled “Latency” is SSH exec-channel round-trip time, not network ping latency.
- The displayed SSH version is obtained from the remote `ssh -V` client, not the connected SSH server, and is not rendered in the system card.
- Capability detection relies on `which` and a shell pipeline instead of portable `command -v` checks.
- Long uptime/load values are forced into one-line metric cards and ellipsized with no detail/copy route.

### Security and privacy

- `EncryptedSharedPreferences`/`MasterKey` APIs are deprecated; migration is not urgent by itself, but key invalidation and corrupted-store failures are not handled and can block credential access without recovery UX.
- Password/private-key strings and temporary byte arrays cannot be reliably zeroized on the JVM and persist for normal object lifetimes; this limitation is undocumented.
- Screen protection is optional globally even on credential-entry and key-generation surfaces.
- No clipboard auto-clear exists for private material.
- First-use trust shows JSch-provided text rather than a structured host/algorithm/fingerprint comparison UI.
- Room contains server hostnames, usernames, notes, tags, and logs and is backed up because only credentials/trust are excluded. This may be intentional, but it is not clearly disclosed as metadata backup.
- The connection-history export can include raw exception/server messages. There is no redaction policy for commands, paths, usernames, or sensitive error content.
- No network-security configuration is present. The app uses SSH/raw sockets rather than HTTP, so this is not an immediate cleartext transport defect, but an explicit no-cleartext policy would protect future HTTP additions.
- The private-key text field can display imported key material and does not force secure-window protection independently.
- No source/git secret-scanning task, dependency vulnerability scan, SBOM, or release provenance step exists in CI.

### Build/release

- Release signing is always configured to a default `my-upload-key.jks` path even when required environment variables/files are absent, likely preventing a normal unsigned local release build.
- ProGuard keeps all JSch classes and members, reducing shrinker effectiveness; the justification should be verified against the maintained fork.
- ProGuard comments mention Crashlytics even though no crash-reporting dependency exists.
- There are no checked-in Room schema exports or migration tests despite `exportSchema = true`.
- The docs say 4.1.0 while the task brief says 4.0 source; release/version truth needs clarification rather than silent renaming.

## 6. Medium-priority defects

- Terminal top and bottom chrome uses a custom `Column` with no safe-drawing/status/navigation insets, causing the reported status-bar and navigation overlap.
- Session status, timestamp, disconnect, and resume/reconnect controls share one row and are crushed at narrow widths/large fonts.
- The Add/Edit Server `AlertDialog` places many fields in a lazy list but still uses a compact modal shell; port/username and auth chips use fixed rows, state is not saveable across configuration, and advanced fields cannot collapse.
- Host validation in the server form only rejects whitespace; it does not validate bracketed IPv6, DNS labels, literal formats, or provide precise errors.
- SFTP rows compress permissions, size, and timestamp into one ellipsized line with no full metadata view.
- Long server address text in the detail header is not explicitly bounded/wrapped.
- Several screens use `collectAsState()` instead of lifecycle-aware collection.
- Top-level app theme/preference flows are also collected without lifecycle awareness.
- The SFTP and terminal activity-scoped ViewModels make process-death restoration and multiple-session behavior opaque.
- Terminal buffer resize copies the upper-left rectangle rather than reflowing or anchoring recent content; this can lose the most relevant bottom rows on shrink.
- Terminal constructors/resize do not guard against non-positive dimensions at the model boundary.
- `LineChart` keys its transition on list size, which is constant during telemetry updates, so later samples do not trigger the intended animation.
- Wake-on-LAN accepts an unvalidated destination hostname/address and fixed port only; it does not distinguish DNS/network/permission errors or explain subnet/hardware limitations in results.
- Ping supports hostnames and IPv4-like strings but the validator rejects IPv6 literals. Android `ping` flag behavior is device/vendor-dependent.
- The lock timeout is hardcoded and not user-configurable; auth errors are displayed but not announced through explicit accessibility live regions.
- The transfer service uses a platform placeholder small icon rather than an app-owned notification asset.
- Deleting multiple remote entries stops at the first failure and provides no per-item result.
- Selection identity is the whole `RemoteFile` value and keys are filename-only; unusual duplicate/changed listings can produce stale selection behavior.

## 7. Low-priority polish issues

- Several large files mix multiple private composables and concerns: `TerminalScreen.kt` (711 lines), `TerminalBuffer.kt` (433), `FileManagerScreen.kt` (421), `SettingsScreen.kt` (410), `ToolsScreen.kt` (405), `ServersScreen.kt` (380), and `ServerDetailScreen.kt` (376).
- `CloudLinkApplication.onCreate()` is an empty override.
- `RemoteFile.size` and timestamp are preformatted strings, preventing locale-aware or alternate presentations.
- Tags are stored as a comma-separated string rather than a normalized model.
- Snippet persistence exists but no current UI consumes it; this is dormant/dead product surface.
- `Resource` is used only for server lookup while other screens use unrelated booleans and nullable errors.
- `DisconnectedStateView` appears unused.
- README refers to a nonexistent `utils` package.
- The settings license dialog is a hand-maintained summary rather than a generated inventory/license artifact.
- UI strings are almost entirely hardcoded in Kotlin, blocking localization and complicating accessibility testing.
- Some comments claim optimization or completeness not supported by measurement (for example “maximum performance” and “strictly optimized”).

## 8. Security risks

### Protected assets

- SSH passwords and private keys
- Host trust/known-host entries
- Server inventory and operational metadata
- Terminal content, commands, remote file content, and transfer destinations
- Connection history and generated key material

### Main threats observed

- First-connection man-in-the-middle if the user accepts an unverified fingerprint
- Secret disclosure through private-key rendering/clipboard/screenshots
- Metadata disclosure through Android backup or plaintext history export
- Remote-file corruption from non-atomic saves/interrupted transfers
- Denial of service from unbounded terminal input/output allocation
- Incorrect session state caused by lifecycle races
- Host-key decision ambiguity caused by parsing human-readable callback text

The app is local-first and contains no analytics/advertising SDK, which materially reduces tracking risk. It is not independently security audited, and rooted-device/debugger/memory-compromise resistance is necessarily limited.

## 9. Performance risks

- Full screen-array copy for each terminal snapshot and full visible-canvas redraw for each update.
- Per-row/per-run string and typeface allocation in the terminal renderer.
- Unbounded outgoing terminal channel and unbounded exec command output strings.
- Ten-thousand-row scrollback can consume several MiB per active terminal; only one terminal buffer currently exists, limiting but not eliminating this cost.
- Repeated polling opens a new exec channel every three seconds and rebuilds history lists.
- SFTP directory `Vector` conversion, mapping, formatting, and sorting happen as one batch.
- Editor keeps both current and saved full-file strings plus encoded copies on save.
- Compose state is split across many individual telemetry flows, causing multiple emissions/recompositions per sample.

No macrobenchmark, baseline profile, allocation benchmark, startup measurement, terminal throughput fixture, or battery/polling measurement exists. Performance claims must remain qualitative until measured.

## 10. Accessibility issues

- Terminal Canvas content has no semantic representation for TalkBack, selection, or copy; this limitation should be disclosed even if a complete accessible terminal is out of immediate scope.
- Quick keys use padding but do not guarantee 48 dp height; several compact icon/text controls are likely below recommended touch size.
- Session and metric layouts are not robust at 1.5×–2.0× font scale.
- Status is often conveyed by color plus text (good), but charts have no semantic description/value.
- SFTP file icons have no description; merged row semantics do not announce file/folder type and metadata together.
- Error messages are not marked as live regions, so asynchronous failures may not be announced.
- Focus order/hardware keyboard behavior is largely implicit and untested.
- Motion is always enabled in the telemetry chart; reduced-motion settings are not considered.
- Dialog/form validation has field-local text for only a subset of errors.
- Most strings are not resources, so locale expansion and translated TalkBack phrasing cannot be evaluated.

## 11. Testing gaps

Existing coverage comprises one context instrumentation test, one trivial Robolectric resource test, four terminal tests, theme role checks, pure parser/formatter tests, and session-list transformation tests. There are no SSH/SFTP fakes, migration tests, repository tests, credential-store tests, connection-state tests, Compose layout tests, service tests, or fixture-based terminal/telemetry suites.

Priority missing tests:

1. Terminal parser: cursor save/restore, erase modes, insert/delete cells and rows, margins, scrollback bounds, alt 1049, multi-parameter modes, SGR reset/bright/256/true color policy, OSC/DCS cancellation, split sequences, CR/LF/tab/backspace, invalid/split Unicode.
2. Terminal buffer: resize, full-region scroll counts, dirty bounds, alternate-screen isolation, cursor/margin clamping, concurrency stress.
3. Terminal ViewModel: connect generation, EOF/error race, exactly-one reconnect, writer failure, bounded backpressure, resize forwarding, cancellation.
4. SSH: duplicate connects, disconnect-during-connect, unknown/matching/mismatched host keys, auth/error mapping, command timeout and output cap, channel cleanup.
5. SFTP: path validation, stale load cancellation, symlinks, partial list errors, atomic edit save, conflict policy, zero-byte and maximum-size files.
6. Transfers: null streams, flush/close before completion, cancellation, service restart/invalid intent, notification state, partial cleanup.
7. Telemetry: Debian/Ubuntu/Fedora/Arch/Alpine/BusyBox/OpenWrt/container fixtures, localized/missing/partial output, per-metric failure.
8. Server credentials: insert/update/auth-type transitions, storage failure rollback/reporting, deletion ordering.
9. Compose: narrow/large-font session card, server form with IME/landscape, terminal safe insets, telemetry wrapping/details, SFTP metadata/details, empty states, lock errors, settings semantics.
10. Room: schema export verification and migration 1→2 with representative legacy schemas/data.

Real infrastructure should be limited to an optional disposable SSH server matrix; automated tests must not contain real credentials.

## 12. Recommended implementation order

1. Capture baseline Gradle tasks/build/unit/lint/Robolectric and device availability without changing failing tests.
2. Make release configuration buildable without private signing material; address any compile/lint blockers found by baseline.
3. Fix credential-auth transition validation and private-key reveal/screen/clipboard handling.
4. Serialize SSH connect/disconnect and introduce explicit per-server connection state plus stable error mapping.
5. Make terminal connect/shell/reconnect generation-safe, bound writer backpressure, and surface write failures.
6. Fix edge-to-edge safe insets and the known session/server-form/telemetry/SFTP responsive layouts.
7. Add terminal parser/buffer regression tests, then correct alternate-screen, mode, erase, color, and Unicode behavior in reviewable increments.
8. Serialize/cancel SFTP UI operations and make remote editor saves atomic where server capabilities allow.
9. Correct transfer completion ordering and add explicit failure/cancel/conflict behavior; document lack of reliable resume if not implemented.
10. Replace telemetry command assumptions with `/proc`-first, portable fallbacks and per-metric result parsing; reduce/pause polling with lifecycle.
11. Add Room migration, repository, ViewModel, service, and Compose adaptive/accessibility tests.
12. Measure terminal throughput/allocation, startup, directory loading, and polling; optimize only confirmed hotspots.
13. Align README/security/architecture/testing/limitations/release documentation with verified behavior, add dependency/license/SBOM guidance, and remove unsupported claims.
14. Run final debug/release build, unit, lint, Robolectric, available instrumentation, secret/TODO/status scans, and document manual device/server tests.

## Manual device and infrastructure testing required

- Status/navigation/cutout/IME insets in portrait, landscape, gesture, and three-button navigation
- 1.3×, 1.5×, and 2.0× font scales on small phone, tablet, split-screen, and foldable-size widths
- Biometric and device-credential flows on API 24–29 and API 30+
- Android 13+ notification denial/approval and foreground-transfer behavior
- Rotation/background/process death during host trust, terminal connect/reconnect, SFTP navigation, editor save, and transfer
- Real SSH host-key first use and mismatch against an independently verified fingerprint
- Real SSH/SFTP against OpenSSH and BusyBox/dropbear-like environments
- Interactive compatibility: vim/neovim/nano/htop/top/tmux/less/watch/mc/systemctl/journalctl, Unicode/wide/combining text, high-volume logs
- Upload/download interruption, conflict, large/zero-byte/binary files, permissions, symlinks, and non-empty directories
- Telemetry on the distributions/environments listed in the project brief
- R8/minified release launch, authentication, SSH key formats, terminal, SFTP, and service notifications

## Audit conclusion

CloudLink has a credible local-first foundation, a compact custom terminal core, sensible dependency choices, and several meaningful security safeguards. It is not yet production-hardened to the level claimed by parts of the documentation. The largest risks are lifecycle races, unbounded/hidden terminal failures, private-key exposure, incomplete terminal semantics, non-atomic SFTP writes, and non-portable all-or-nothing telemetry. These should be corrected with focused state/lifecycle changes and regression tests rather than a wholesale rewrite.
