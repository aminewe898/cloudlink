# Release Checklist

## Source and dependency review

- [ ] Review every change and exported Room schema. This supplied folder has no Git metadata, so use a real repository/worktree for release review.
- [ ] Search source, resources, CI, documentation, and history for credentials/private keys.
- [ ] Review `gradle/libs.versions.toml` against official release notes and vulnerability databases; test dependency upgrades rather than applying lint suggestions blindly.
- [ ] Generate and archive an SBOM/dependency report and complete third-party license notices.
- [ ] Confirm `versionCode`, `versionName`, target SDK, privacy disclosures, and changelog.

## Automated gates

```powershell
.\gradlew.bat testDebugUnitTest lint assembleDebug assembleRelease --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
```

- [ ] Unit/Robolectric tests pass with no skips introduced to hide failures.
- [ ] Lint reports zero errors; warnings are triaged.
- [ ] Debug and minified release variants assemble.
- [ ] Instrumentation tests run on at least one minimum/old API device and one current API device.
- [ ] Room migration tests pass from every supported schema version.

## Signing

Set secrets in the CI/release environment, never in source or a committed properties file:

- `KEYSTORE_PATH`: absolute path to the release/upload keystore
- `STORE_PASSWORD`: keystore password
- `KEY_ALIAS`: signing alias (defaults to `upload` only when the other values are present)
- `KEY_PASSWORD`: key password

With missing/invalid signing inputs, CloudLink intentionally creates `app-release-unsigned.apk`. That file is not a production deliverable.

- [ ] Build the signed release in a controlled environment.
- [ ] Verify APK/AAB signer certificate and compare its digest to the authorized release certificate.
- [ ] Install the actual minified signed artifact, authenticate, connect, open terminal/SFTP, and exercise a foreground transfer.
- [ ] Archive mapping files, native symbols, SBOM, checksums, source revision, toolchain versions, and signed artifacts with restricted access.

## Manual security/functional gates

- [ ] Independently verify a new host fingerprint and confirm changed keys are blocked.
- [ ] Test password and private-key create/edit/auth-type transition/delete flows.
- [ ] Confirm sensitive dialogs block screenshots and generated keys disappear when cleared/navigation changes.
- [ ] Test clipboard timeout and document clipboard residual risk.
- [ ] Test backgrounding, process death, rotation, lock timeout, notification denial, and transfer cancellation.
- [ ] Complete the terminal, SFTP, telemetry, adaptive UI, and accessibility matrices from `TESTING.md`.

## Distribution

- [ ] Confirm package ID and Play/app-store signing path.
- [ ] Verify backup/data-safety declarations match `SECURITY_MODEL.md`.
- [ ] Publish release notes including known limitations and migrations.
- [ ] Confirm the private vulnerability mailbox is monitored.
