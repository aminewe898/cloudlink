# Changelog

All notable changes to this project will be documented in this file.

## [v3.4.0] - Release Candidate
### Added
- Complete UI/UX Polish.
- "Modern Dark" professional Slate theme as default.
- Scoped Terminal and SFTP ViewModels to the Activity lifecycle for persistent sessions across tabs.
- Added native IME padding and keyboard tap-to-focus for the Terminal view.

### Fixed
- Fixed a massive UI stuttering bug in the VT100 terminal by implementing a 60FPS Conflated render pipeline.
- Fixed a severe bug where the SFTP download foreground service was crashing due to Android 14 notification intent rules.
- Fixed an SSH pipeline race condition where `ChannelShell` streams were requested before PTY initialization was complete, which previously caused the keyboard to freeze.

## [v3.3.0]
### Added
- Added Advanced SFTP operations (Move, Copy, Rename, Create File, Create Folder, Chmod).
- Implemented Android Foreground Service for persistent background file transfers.

## [v3.2.0]
### Changed
- Rebuilt the entire Terminal architecture from a standard String buffer to an explicit `VirtualTerminal` 2D Array capable of parsing ANSI escape codes and cursor manipulation.
- Support for `nano` and `vim`.

## [v3.1.0]
### Added
- Real-time CPU, RAM, Disk, and Network telemetry parsing.
- Added fluid LineChart graphing.
- Added capability detection chips (Docker, Nginx).

## [v3.0.0]
### Added
- Complete rewrite of the application using Jetpack Compose and MVVM architecture.
- Migrated away from deprecated JSch features to a modernized repository pattern.

---

## [v2.0.0]
- Initial implementation of the server Dashboard.
- Basic SSH execution (Non-interactive).
- Simple file listing for SFTP.

## [v1.0.0]
- Prototype launch.
