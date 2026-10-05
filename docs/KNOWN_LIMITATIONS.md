# Known Limitations

These are verified product boundaries, not hidden implementation promises.

## Terminal

- The parser implements a practical VT/ANSI subset, alternate-screen modes, cursor-key mode, bracketed paste, 256 colors, and a nearest-256-color approximation for true-color SGR input. It is not a complete xterm implementation.
- Supplementary Unicode code points are preserved, but wide-character column width, combining marks, emoji grapheme clusters, bidirectional text, and complex shaping are not fully modeled. CJK/emoji alignment can be wrong.
- Resize preserves a rectangular cell region; it does not reflow historical terminal text.
- The Canvas exposes a description to accessibility services but not a navigable/copyable semantic representation of every terminal cell.
- There is one activity-scoped terminal workspace, not multiple tabs or persistent remote sessions.

## SSH and keys

- Host trust uses TOFU and requires independent first-fingerprint verification for strong identity assurance.
- Generated keys are 4096-bit RSA. Generated private keys are shown on demand and are not automatically saved to a server profile.
- Encrypted/passphrase-protected private keys are not supported because CloudLink does not persist passphrases.
- ProxyJump, SSH agents, port forwarding, certificates, FIDO/security keys, and per-host algorithm configuration are not implemented.

## SFTP and editor

- Transfers are serialized in one foreground service instance. There is no durable/resumable queue across process death or reboot.
- Upload refuses to overwrite an existing remote target. The editor uses a temporary sibling plus rename and fails safely if replacement is unsupported; server-specific rename semantics still apply.
- The editor is limited to text-like, non-symlink files up to 2 MiB. It rejects detected binary content.
- Recursive directory upload/download, ownership changes, ACLs, extended attributes, and reliable cross-filesystem atomic moves are not supported.

## Telemetry and tools

- Telemetry is best effort and `/proc`-first. Hardened containers, non-Linux systems, missing tools, localized variants, and restricted permissions may leave individual metrics unavailable.
- CPU data depends on tools exposed by the remote environment; RAM, uptime, load, and disk metrics can remain available independently.
- “SSH round trip” is command round-trip time, not ICMP latency.
- Ping behavior and flags depend on the Android device image. Wake-on-LAN depends on subnet routing, broadcast policy, and target hardware configuration.

## Platform and release

- Minimum Android version is API 24. Older versions are unsupported.
- The pre-API-30 device-credential fallback and direct system-bar coloring use deprecated compatibility APIs by necessity/current implementation.
- A production release needs an external keystore and environment variables; the verified local artifact is intentionally unsigned.
- No reproducible-build attestation, generated SBOM, dependency lockfile, macrobenchmark, or published independent security audit is included yet.
