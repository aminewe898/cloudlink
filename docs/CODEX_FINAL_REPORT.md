# CloudLink Hardening Report

Date: 2026-08-03

## Outcome

CloudLink now builds in debug and minified release modes, passes Android lint with zero errors, and passes 32 fresh unit/Robolectric tests. The local release is intentionally unsigned because no production keystore was supplied. Connected instrumentation remains unexecuted because uncached Unified Test Platform dependencies could not be resolved in the available network environment.

## High-impact fixes

- Release: conditional external signing, Room schema export, and narrow R8 annotation rules unblock an honest unsigned release build without embedding signing defaults.
- SSH: explicit connection phases/failures, per-server serialization and epochs, disconnect-during-connect safety, private-key validation, known-host inspection/removal, and synchronized identity changes.
- Terminal: generation-safe shell/reconnect lifecycle, bounded writer, visible writer failures, application cursor mode, DSR/DA responses, multi-mode parsing, insert mode, alternate-screen/ED3 corrections, true-color approximation, parser caps, hardware keys, safe insets, and renderer allocation reductions.
- Credentials: checked encrypted preference commits, create/update rollback, auth-transition credential requirement, and surfaced CRUD failures.
- SFTP: serialized/cancellable UI operations, symlink-safe deletion, per-item delete failures, bounded text editor, binary/symlink rejection, temp-plus-rename save, permission preservation, transfer conflict refusal, stream completion ordering, notification permission, and cancel action.
- Telemetry: marked locale-independent output, `/proc`-first portable metrics, partial-metric parsing, explicit unavailable states, lifecycle polling pause, and capped histories.
- Security/UI: no cleartext traffic, secure sensitive windows, confirmed private-key reveal and timed clipboard cleanup, host prompt hidden above app lock, adaptive server form/session/telemetry/file layouts, full metadata dialog, minimum touch sizing, lifecycle-aware state collection, and hardware keyboard support.

## Verification

| Gate | Final result |
|---|---|
| `assembleDebug` | Passed |
| Fresh `testDebugUnitTest --rerun-tasks` | Passed: 32 tests, 0 failures/errors/skips |
| `lint` | Passed: 0 errors; 32 warnings/advisories remain |
| `assembleRelease` with R8 | Passed; unsigned artifact |
| Pixel 6a install/launch smoke | Passed: debug APK installed with data preserved, cold launch succeeded, process stayed alive, no immediate AndroidRuntime crash |
| Connected instrumentation | Blocked before execution by unavailable Maven/UTP resolution |
| Source credential/TODO pattern scan | No embedded credential; one expected runtime password lookup match |
| Git history/status review | Unavailable: supplied directory is not a Git worktree |

Artifacts at verification time:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` — 21,332,690 bytes
- Minified release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` — 3,420,058 bytes
- Lint HTML: `app/build/reports/lint-results-debug.html`
- Test HTML: `app/build/reports/tests/testDebugUnitTest/index.html`
- Room schemas: `app/schemas/`

## Remaining release blockers

1. Supply a controlled release keystore and verify/install the signed minified artifact.
2. Restore Maven access and execute instrumentation on multiple API levels; the install/launch smoke test is not a substitute.
3. Complete the real SSH/SFTP/distribution/terminal matrix in `TESTING.md`.
4. Upgrade or migrate AndroidX Security Crypto and upgrade JSch in isolated, fully tested changes.
5. Add Room migration, credential-store, SSH/SFTP fake/service, adaptive Compose, process-death, and performance benchmark coverage.
6. Generate complete SBOM/license artifacts and add dependency verification/locking.
7. Validate remaining deprecated compatibility paths and malformed launcher-density warnings on real launchers.

The app is materially safer and more deterministic than the supplied baseline, but it should not be described as fully production-certified until these external/device/infrastructure gates are complete.
