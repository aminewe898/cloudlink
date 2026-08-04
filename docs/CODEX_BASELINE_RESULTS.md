# CloudLink Baseline Results

Baseline date: 2026-08-03
Baseline point: after `docs/CODEX_PROJECT_AUDIT.md`, before any production-code change

## Environment

- Host: Windows, PowerShell
- Project: single Gradle module `:app`
- JDK used: Android Studio bundled JBR at `C:\Program Files\Android\Android Studio\jbr`
- Android SDK: `C:\Users\amine\AppData\Local\Android\Sdk`
- Gradle wrapper: 9.3.1
- Android Gradle Plugin: 9.1.1
- Connected device: Pixel 6a (`bluejay`, serial redacted here; `adb` reported it as online)
- Initial shell state: `JAVA_HOME` and `java` were not configured on `PATH`; `adb` was not on `PATH`. Commands below set `JAVA_HOME` only for the command process and invoked SDK `adb` by absolute path. No system setting was changed.
- Repository metadata: the supplied directory is not a Git worktree (`git status` reported “not a git repository”), so commit/history secret scanning and a Git diff baseline are unavailable.

## Task discovery

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat tasks --all --no-daemon
```

Result: **passed** in 32 seconds.

Relevant discovered tasks:

- `app:assembleDebug`
- `app:assembleRelease`
- `app:test` / `app:testDebugUnitTest`
- `app:lint`, `app:lintDebug`, `app:lintRelease`, `app:lintVitalRelease`
- `app:connectedDebugAndroidTest`, `app:connectedAndroidTest`
- `app:assembleDebugAndroidTest`

Gradle warned that `android.disallowKotlinSourceSets=false` is experimental.

## Debug build

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug --no-daemon --stacktrace
```

Result: **passed** in 50 seconds.

Artifact:

- `app/build/outputs/apk/debug/app-debug.apk`
- Baseline size: 21,237,752 bytes

Environment warning: the installed SDK processing library understands SDK XML up to version 3 but encountered version 4, indicating Android Studio/command-line-tools skew.

## JVM unit and Robolectric tests

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat test --no-daemon --stacktrace
```

Result: **passed** in 2 minutes 15 seconds.

Test summary: 16 tests, 0 failures, 0 errors, 0 skipped.

| Suite | Tests |
|---|---:|
| `CloudLinkRobolectricTest` | 1 |
| `TerminalBufferTest` | 4 |
| `ThemeSelectorTest` | 2 |
| `DashboardViewModelTest` | 3 |
| `SessionsViewModelTest` | 2 |
| `SettingsViewModelTest` | 1 |
| `ToolsViewModelTest` | 3 |

The only Robolectric test reads the application name resource. There are no existing Robolectric UI/layout assertions.

## Android lint

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat lint --no-daemon --stacktrace
```

Result: **failed** in 2 minutes 13 seconds with **4 errors and 33 warnings**.

Errors:

1. `ToolsViewModel.kt:48`: `Process.waitFor(long, TimeUnit)` requires API 26; minSdk is 24.
2. `ToolsViewModel.kt:50`: `Process.destroyForcibly()` requires API 26.
3. `ToolsViewModel.kt:117`: `Process.destroyForcibly()` requires API 26.
4. `TerminalScreen.kt:734`: invalid `Typeface` constant/flag expression (`WrongConstant`).

Warning categories:

- One redundant activity label.
- New Gradle/AGP/dependency versions available. These are informational and must not be applied blindly.
- Launcher density warnings report nonsensical effective xxhdpi/xxxhdpi dimensions despite small file byte sizes; launcher image encoding/metadata needs inspection.
- `cloudlink_icon_fg.jpg` is in the densityless `drawable` directory.
- Three `SharedPreferences.edit()` KTX suggestions.
- One `String.toUri()` KTX suggestion.

Reports:

- `app/build/reports/lint-results-debug.html`
- `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`

## Connected instrumentation test

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat connectedDebugAndroidTest --no-daemon --stacktrace
```

Result: **blocked / failed before test execution** after 4 minutes 32 seconds.

The Pixel 6a was online, but Gradle could not resolve uncached Unified Test Platform dependencies (`netty`, `gson`, Kotlin stdlib common, and related artifacts) because `repo.maven.apache.org` DNS/network access was unavailable. Configuration-cache serialization then reported the unresolved UTP file collection. The single instrumentation test was not installed or executed, so it must not be reported as passed.

## Release build and R8

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleRelease --no-daemon --stacktrace
```

Result: **failed** in 2 minutes 25 seconds at `:app:minifyReleaseWithR8`.

R8 missing classes:

- `com.google.errorprone.annotations.CanIgnoreReturnValue`
- `com.google.errorprone.annotations.CheckReturnValue`
- `com.google.errorprone.annotations.Immutable`
- `com.google.errorprone.annotations.RestrictedApi`

These are referenced by Google Tink transitively used by AndroidX Security. Generated suggestions are in `app/build/outputs/mapping/release/missing_rules.txt`. No release APK was produced. The build did not reach the expected missing-local-keystore validation, so signing behavior remains a second release blocker after R8.

Release compilation warnings:

- Room schema export is enabled but no schema output directory/plugin argument is configured.
- Deprecated JSch `removeIdentity(String)` calls (3).
- Deprecated non-auto-mirrored icons in File Manager (2) and Terminal (1).
- Deprecated pre-API-30 device-credential intent path in Lock Screen (required as compatibility fallback unless reworked).
- Deprecated direct navigation-bar color assignment.
- Native libraries `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` could not be stripped and were packaged unchanged before R8 failed.

## Manifest, backup, and release observations

- Manifest component exposure is minimal: launcher activity exported, transfer service non-exported.
- Permissions are INTERNET, network state, biometric, foreground service/data sync, and notifications; no broad storage permission is present.
- `allowBackup=true` is enabled.
- Encrypted credential preferences and `known_hosts` are excluded from legacy backup, Android 12+ cloud backup, and device transfer.
- The Room database (server metadata and logs) and theme preferences remain eligible for backup.
- There is no network-security configuration.
- Release minification is enabled, but the baseline release does not complete.
- No release signing secret is present in source. The script defaults to `${rootDir}/my-upload-key.jks` when `KEYSTORE_PATH` is unset; that file was not observed in the repository inventory.

## Baseline status summary

| Check | Status |
|---|---|
| Gradle task discovery | Passed |
| Debug APK build | Passed |
| JVM unit tests | Passed (16/16) |
| Robolectric | Passed (1 trivial resource test) |
| Android lint | Failed (4 errors, 33 warnings) |
| Connected instrumentation | Blocked before execution by unavailable UTP dependencies/network |
| Minified release APK | Failed at R8 missing annotation classes |
| Git status/history scan | Unavailable; supplied folder is not a Git worktree |

No failing tests were deleted, skipped, or hidden to create this baseline.
