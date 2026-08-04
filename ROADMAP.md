# CloudLink Product Roadmap

CloudLink should become an excellent mobile operations console, not a crowded general-purpose admin panel. Features are ordered by the value they add to real SSH/SFTP work.

## Priority 0 — Reliability before expansion

- **Terminal compatibility suite:** Test and harden Vim, Neovim, tmux, htop, less, nano, Unicode wide/combining characters, resize behavior, alternate screen, and common DEC/xterm modes.
- **Terminal selection and search:** Add touch selection handles, copy, find-in-scrollback, and a configurable scrollback limit.
- **Transfer control:** Add visible progress, cancellation, retry, conflict handling, resumable large transfers, and a persistent transfer queue.
- **Real-device test matrix:** Cover small phones, tablets, foldables, hardware keyboards, Android 8 through current Android, slow networks, connection loss, and rotation.
- **Known-host export/comparison:** Inspection and intentional removal are implemented; add export and a dedicated out-of-band comparison workflow.

## Priority 1 — Power-user workflows

- **Multi-session workspace:** Keep several SSH sessions alive with tabs, session switching, rename, duplicate, and close controls.
- **Background sessions:** Use an Android foreground service so long-running jobs survive app backgrounding, with an explicit persistent notification.
- **Command palette and snippets:** Manage saved commands, search them quickly, use variables, preview before execution, and scope dangerous snippets to confirmation.
- **Advanced hardware keyboard support:** Core arrows/navigation/Ctrl/Alt input is implemented; add function-key completeness, configurable shortcuts, and future tab switching.
- **SSH tunneling:** Add local, remote, and dynamic SOCKS forwarding with clear active-tunnel indicators.
- **Tablet workspace:** Add server list + terminal/files split views, draggable panes, and adaptive layouts.

## Priority 2 — Operations intelligence

- **Health history:** Persist CPU, memory, storage, latency, and uptime samples with useful time ranges rather than only live snapshots.
- **Alerts:** Support local thresholds for disk, load, temperature, memory, and connection failures with quiet hours and per-server controls.
- **Service dashboard:** Detect systemd, Docker, and common package managers, then expose safe status and log views before any mutation controls.
- **Audit trail:** Record connection, transfer, trust, and command-snippet events locally with export and retention controls.
- **Quick actions:** Add Android shortcuts and widgets for favorite servers, Wake-on-LAN, and opening a named session.

## Priority 3 — Optional ecosystem features

- **Encrypted backup/export:** Export servers, snippets, preferences, and trust data into a user-protected encrypted archive.
- **End-to-end encrypted sync:** Optional multi-device sync where the service never receives plaintext credentials or private keys.
- **Plugin-style tool panels:** Keep Docker, Kubernetes, logs, and service controls optional so the core SSH client remains fast and understandable.

## Recently completed in 4.1

- Operations-console visual system and primary navigation redesign.
- Fleet overview and richer server workspace cards.
- Explicit terminal connection and reconnect states.
- Adjustable terminal text, live/scrollback feedback, jump-to-live, safe multiline paste, bracketed paste, ALT/function quick keys, and mode-preserving local clear.
- Known-host inspection/removal, connection race protection, bounded terminal writes, adaptive server/SFTP/session layouts, atomic editor saves, and partial portable telemetry.
