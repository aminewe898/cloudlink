# CloudLink Security Model

Last reviewed: 2026-08-03

## Assets and trust boundaries

CloudLink protects SSH passwords/private keys, accepted host keys, server inventory, terminal output, remote files, and connection history. The Android application sandbox, Android Keystore, device unlock state, remote SSH server, and first-use host-key decision are separate trust boundaries.

## Implemented controls

- Credentials are encrypted at rest with AES-256-GCM through AndroidX Security and a master key held by Android Keystore. Hardware backing depends on the device and is not attested by CloudLink.
- Credentials are excluded from Room, cloud backup, and device transfer. Writes/deletes use synchronous checked commits so storage failure reaches the caller.
- Server create/update operations validate credentials and roll back profile changes if secure storage fails. Authentication-type changes require replacement credentials.
- SSH uses trust on first use. New host keys require an explicit prompt; changed/mismatched keys are rejected. Settings exposes accepted hosts and exact-entry removal.
- Cleartext Android networking is disabled. SSH/SFTP is direct to the configured endpoint; the app contains no analytics or advertising SDK.
- The app lock accepts a strong biometric or device credential. Host-key content is not displayed above the lock route.
- Server credential forms and generated-private-key views force `FLAG_SECURE`. Generated private keys are hidden behind confirmation and are never stored automatically.
- Copying a generated private key places it on the clipboard for at most 60 seconds when CloudLink can verify the clipboard is unchanged.
- The transfer service is non-exported, uses content URIs rather than broad storage permission, and provides an explicit cancellation action.
- Release signing values are read only from environment variables. With no valid keystore, Gradle produces an unsigned release artifact instead of falling back to a repository path or debug key.

## Host-key workflow

TOFU is not identity proof. Before accepting a first connection, compare the displayed fingerprint with a value obtained through an independent trusted channel. A removed trust entry causes the next connection to prompt again. A changed key must be investigated; CloudLink does not offer an automatic bypass.

## Residual risk

- A compromised/rooted device, malicious accessibility service, debugger, or process-memory reader can defeat app-level protections.
- The lock is an application access gate; it does not cryptographically bind every credential read to a fresh biometric operation.
- Clipboard managers or keyboards may observe data before clipboard cleanup. Prefer importing a key directly into its destination.
- `known_hosts` is integrity-protected by the Android sandbox, not encrypted.
- TOFU remains vulnerable if an attacker controls the very first connection and the user does not verify the fingerprint.
- AndroidX Security Crypto is currently pinned at `1.1.0-alpha06`; upgrading or migrating to direct Keystore use is a release-priority dependency task. Its later stable API is deprecated in favor of platform/Keystore primitives.
- JSch `0.2.20` is behind the maintained fork's current line. An upgrade requires algorithm/interoperability regression tests and should not be performed as an unverified version-only edit.
- SFTP editor saves depend on server rename semantics. The app avoids truncating an existing destination when replacement is unsupported, but cannot guarantee cross-filesystem atomicity.

## Disclosure

Report suspected vulnerabilities privately to:

Mohamed Amine Aslimani<br>
Founder and Developer — CloudLink / Zxeon Tech<br>
anaslimani923@gmail.com

Do not include live credentials, private keys, or sensitive server output in a report.
