# Frequently Asked Questions (FAQ)

### What is CloudLink?
CloudLink is a premium, open-source Android application for managing Linux servers remotely. It provides SSH access, SFTP file management, and various network tools inside a native, hardware-accelerated Jetpack Compose interface.

### Is my server data secure?
CloudLink encrypts passwords and private keys in Android's `EncryptedSharedPreferences` using AES256-GCM with a master key held by Android Keystore. Hardware backing depends on the device, and a rooted/compromised device remains outside the protection model. The application does not include third-party telemetry reporting.

### Can I run terminal applications like `vim`, `nano`, or `htop`?
Many common applications work because CloudLink supports PTY sizing, alternate screens, cursor modes, bracketed paste, supplementary Unicode code points, and xterm 256 colors. It is not a complete xterm emulator: wide/combining/emoji layout and less-common control sequences can render incorrectly. Test critical workflows against your environment.

### How does the SSH Key Generator work?
The SSH Key Gen tool creates a local 4096-bit RSA key pair. It displays the public key and fingerprint and hides the private key behind an explicit confirmation. CloudLink does not save a generated pair automatically; copy/import it deliberately and clear it when finished.

### Why doesn't the keyboard appear when I tap the terminal?
We recently patched an issue where the Android software keyboard (IME) would fail to deploy. CloudLink now forcibly summons the keyboard via the `LocalSoftwareKeyboardController` whenever the terminal canvas is tapped. If it still doesn't appear, ensure you aren't using a hardware keyboard or a conflicting third-party IME.

### Can I edit files over SFTP?
Yes. The built-in File Manager allows you to navigate the remote filesystem, and the integrated Code Editor lets you open, modify, and save text files directly back to your server.

### Does it support biometric authentication?
Yes. CloudLink uses Android's `BiometricPrompt` and accepts a strong enrolled biometric or the device PIN, pattern, or password. This protects app access but does not make a rooted or compromised device trustworthy.
