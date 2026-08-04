# Dependency and Supply-Chain Review

Reviewed: 2026-08-03

Versions are centralized in `gradle/libs.versions.toml`; Gradle resolves transitive dependencies. The project currently has neither dependency verification metadata nor a committed lockfile/SBOM, so a release should add those controls.

## Security-relevant findings

| Dependency | Pinned version | Finding/action |
|---|---:|---|
| AndroidX Security Crypto KTX | 1.1.0-alpha06 | Old alpha. The official stable 1.1.0 exists, but its API is deprecated in favor of platform APIs/direct Keystore. Plan a tested stable upgrade or direct-Keystore migration; do not leave this unowned. |
| mwiede JSch | 0.2.20 | Maintained fork, but substantially behind its current release line. Upgrade in a dedicated branch with SSH algorithm, key-format, host-key, SFTP, Android API, and R8 tests. |
| AndroidX Biometric | 1.1.0 | Current stable remains 1.1.0; newer Compose-facing work is alpha. Keeping stable is appropriate. |
| AndroidX/Compose/Room/lifecycle/navigation | mixed stable pins/BOM | Lint reports available upgrades. Apply coherent, release-note-driven groups and run UI/migration/R8 tests. |
| JUnit 4 | 4.13.2 | Test-only; keep isolated from production runtime. |

Official references: [AndroidX Security releases](https://developer.android.com/jetpack/androidx/releases/security), [AndroidX Biometric releases](https://developer.android.com/jetpack/androidx/releases/biometric), [AndroidX version table](https://developer.android.com/jetpack/androidx/versions), and [mwiede/JSch releases](https://github.com/mwiede/jsch/releases).

No dependency was blindly upgraded during this offline verification pass: only pinned versions already in the local cache could be compiled and tested, and Maven/UTP resolution was unavailable. “No advisory observed” is not proof of absence; automate an OSV/GitHub Advisory/Dependabot scan in a networked CI environment.

## Required release controls

1. Enable Gradle dependency verification and review checksums/signatures.
2. Generate CycloneDX or SPDX SBOMs for the resolved release runtime and archive them per release.
3. Run a Maven-ecosystem vulnerability scan on direct and transitive packages.
4. Generate full notices/license texts from the resolved graph. `NOTICE` and `CREDITS.md` are summaries, not a complete compliance artifact.
5. Pin GitHub Actions by immutable commit SHA for stronger workflow supply-chain control.
6. Configure dependency-update automation with grouped AndroidX/Compose changes and mandatory build/test/lint gates.
7. Review R8 missing-class rules after every security/crypto or SSH upgrade; avoid broad `-dontwarn` rules unless the referenced annotation-only classes are verified optional.

## Upgrade order

1. Security Crypto stable/migration, with credential read/write/upgrade/backup tests.
2. JSch latest supported line, with disposable real-server interoperability matrix.
3. Lifecycle/navigation/activity/Room coordinated stable updates.
4. Compose BOM and UI tests at compact/expanded widths.
5. AGP/Kotlin/KSP/Gradle as a coordinated toolchain change.
