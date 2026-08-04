# CloudLink Architecture

CloudLink is a single-module, local-first Android application. Jetpack Compose renders the UI, Hilt provides process- and ViewModel-scoped dependencies, Room stores non-secret server data, DataStore stores preferences, and AndroidX Security stores SSH credentials outside Room.

## Runtime flow

1. `MainActivity` starts at the lock route, applies edge-to-edge/theme/privacy settings, and owns the global host-key confirmation surface.
2. `ServerRepository` reads and writes profiles and connection history in Room. Passwords and private keys never enter Room.
3. `CredentialManager` encrypts credentials with a Keystore-backed master key in `EncryptedSharedPreferences`.
4. `SshConnectionManager` serializes connection creation per server, owns live JSch sessions, publishes explicit connection phases, validates key material, and maintains the private `known_hosts` file.
5. Feature ViewModels open shell, exec, or SFTP channels on a shared authenticated session. Blocking transport and parsing work runs off the main thread.
6. Compose screens collect state with lifecycle awareness and adapt core layouts to compact widths, safe drawing insets, and the IME.

## Major boundaries

| Boundary | Responsibility |
|---|---|
| `data/database` | Room entities, DAOs, migrations, exported schemas |
| `data/security` | Encrypted credential persistence and atomic replacement |
| `data/network` | SSH session lifecycle, host trust, channels, error classification |
| `terminal` | Packed cell model, screen/scrollback buffer, VT/ANSI stream parser |
| `ui/viewmodel` | Feature orchestration, cancellation, reconnect policy, UI state |
| `ui/screens` | Compose presentation, input, adaptive layout, user confirmation |
| `SftpTransferService` | Foreground SAF-to-SFTP streaming with cancellation notification |

## Lifecycle invariants

- One connection mutex and monotonically increasing epoch exist per server. A disconnect invalidates any in-flight connect before it can publish a live session.
- A terminal connection generation owns its shell reader, bounded writer queue, and at most one reconnect job. Work from an old generation cannot update the new session.
- Interactive SFTP operations are serialized; changing directories cancels stale listing work.
- Telemetry polling is paused when the server-detail destination leaves composition.
- A terminal is considered connected only after its shell channel is connected, not merely after base SSH authentication.

## Storage and backup

- Room: server profiles, non-secret metadata, and connection history.
- Encrypted preferences: passwords and private keys, keyed by server ID.
- Internal file: OpenSSH-style host-trust data.
- DataStore: theme and privacy preferences.
- Credential preferences and `known_hosts` are excluded from backup and device transfer. Server metadata may be backed up under the platform policy.

## Deliberate constraints

The current app has one activity-scoped terminal state holder and one interactive SFTP state holder. It does not provide concurrent terminal tabs, durable transfer queues, remote process supervision, or a domain-level transport abstraction. These constraints are documented in `KNOWN_LIMITATIONS.md` and should be changed only with lifecycle and persistence tests.
