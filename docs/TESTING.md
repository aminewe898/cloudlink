# Testing CloudLink

## Local prerequisites

- Android Studio JBR or another compatible JDK 17
- Android SDK API 36
- Network access for dependencies not already present in the Gradle cache
- A physical/emulated Android device for instrumentation

Windows PowerShell example:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\amine\AppData\Local\Android\Sdk'
.\gradlew.bat testDebugUnitTest lint assembleDebug assembleRelease --no-daemon
```

## Automated coverage

The unit/Robolectric suite covers terminal buffer and parser behavior, explicit SSH error/state mapping, telemetry parsing with missing metrics, responsive form validation, IPv6 tool validation, theme roles, session mapping, and settings basics. It does not open real network connections.

Room schemas are exported to `app/schemas`. Schema files should be reviewed and committed with every database change. Add migration tests before changing the database version.

Instrumentation is intentionally separate:

```powershell
.\gradlew.bat connectedDebugAndroidTest --no-daemon
```

The supplied environment had an online Pixel 6a, but Gradle could not resolve uncached Unified Test Platform artifacts because Maven DNS/network access was unavailable. A separate debug APK install/cold-launch smoke check succeeded without an immediate AndroidRuntime crash; no instrumentation test is claimed as passed until the Gradle command actually executes the tests.

## Real SSH/SFTP matrix

Use disposable accounts and servers. Do not place credentials in test sources or Gradle properties.

- OpenSSH on Debian/Ubuntu, Fedora, Arch, and Alpine
- BusyBox/dropbear-like systems and containers with partial `/proc`
- Password and supported unencrypted private-key authentication
- First-use host key, matching reconnect, deliberately changed key, and removed trust
- Slow handshake, auth failure, network loss, background/foreground, rotation, and user disconnect during connect
- `vim`, `nano`, `less`, `top`/`htop`, `tmux`, high-volume logs, cursor mode, alternate screen, colors, and bracketed paste
- SFTP zero-byte/large/binary files, conflicts, symlinks, permissions, non-empty directories, cancellation, and interrupted transfers
- Android 13+ notification grant and denial

## UI/accessibility matrix

Verify portrait and landscape, gesture and three-button navigation, display cutouts, hardware keyboard, split screen, phone/tablet widths, and font scales 1.0×/1.5×/2.0×. Use TalkBack and switch access. Pay particular attention to the terminal Canvas: it has a descriptive semantic node but not a full accessible text model.

## Release acceptance

A release candidate must pass the checklist in `RELEASE_CHECKLIST.md`, including installation and launch of the minified, signed artifact. `assembleRelease` without signing variables is only an R8/package verification step and produces an unsigned APK.
