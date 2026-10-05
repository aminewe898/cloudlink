# Security Overview & Policy

Security is the foundational pillar of CloudLink. As an application designed to hold root-level access credentials for remote infrastructure, we take threat modeling and data protection extremely seriously.

This document outlines the current security architecture verified directly from the implementation logic.

## 1. Credential Storage & Encryption
**Implementation:** `CredentialManager.kt`

CloudLink does **not** store plaintext passwords or private keys in the local SQLite database.

All sensitive authenticators are handled by the AndroidX Security library `EncryptedSharedPreferences`.
- **Encryption:** Values are encrypted at rest using `AES256-GCM`.
- **Key Management:** The master key (`AES256_GCM`) is held by Android Keystore. Whether that key is hardware-backed depends on the device.
- **Keyspace:** Passwords and private keys are keyed uniquely per server ID (e.g., `pwd_<id>`, `key_<id>`).
- **Backup Policy:** Encrypted credential preferences and SSH host-trust data are excluded from cloud backup and device transfer.

## 2. Authentication & App Lock
**Implementation:** `LockScreen.kt`

Access to the application is restricted by a mandatory application lock screen.
- **Authentication Integration:** CloudLink uses Android's `BiometricPrompt` API and accepts a strong biometric or the device PIN, pattern, or password. This is an application access gate; it is not a cryptographic unlock of each stored credential.

## 3. Host Key Verification
**Implementation:** `SshConnectionManager.kt`

CloudLink implements a **Trust on First Use (TOFU)** model.
- Upon connecting to a new server, CloudLink presents JSch's host-key verification message for user confirmation, then stores an accepted key in the application's private sandbox (`context.filesDir/known_hosts`).
- A changed or mismatched host key is rejected without offering an unsafe automatic override.

## 4. Screenshot & Screen Recording Protection
CloudLink respects the sensitivity of terminal outputs. Global screen protection is configurable, while credential forms and generated-private-key surfaces force `FLAG_SECURE` even when the global preference is off.

## 5. Network Privacy
CloudLink is entirely self-contained.
- **No Telemetry:** We do not track, log, or transmit usage statistics, connection metadata, or crash reports to external servers.
- **Direct Connections:** All SSH and SFTP connections are negotiated directly between your Android device and the target server.

## 6. Known Limitations
- The `Trust on First Use` model still depends on the user validating the first-connection fingerprint through a trusted channel.
- Android Keystore hardware backing is device-dependent and is not attested by the application.
- The app lock is an access gate and does not cryptographically require a fresh biometric operation for every credential use.
- Generated private keys can still be observed by clipboard managers or a compromised device before timed clipboard cleanup.
- The terminal is a practical VT/ANSI subset; security-sensitive full-screen prompts should be independently verified if rendering appears unusual.

## Reporting a Vulnerability

If you discover a security flaw or cryptographic weakness in CloudLink, **DO NOT** open a public issue on GitHub.

Please disclose it responsibly to:

Mohamed Amine Aslimani<br>
Founder and Developer — CloudLink / Zxeon Tech<br>
anaslimani923@gmail.com

Do not attach live credentials, private keys, or sensitive server output. See [`docs/SECURITY_MODEL.md`](docs/SECURITY_MODEL.md) for the full threat model.
