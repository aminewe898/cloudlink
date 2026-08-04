# Contributing to CloudLink

We welcome and appreciate contributions from the community! Because CloudLink aims for production-grade commercial stability, we ask that you follow these guidelines before submitting code.

## 1. Core Philosophy
*   **Stability over Features:** CloudLink is an SSH/SFTP client, not a swiss-army knife. If a feature does not directly aid in remote server administration, it likely does not belong here.
*   **No Placeholders:** Do not merge UI code that has no underlying implementation. If a button is visible, it must work.
*   **Performance is Critical:** The terminal engine runs on a 60 FPS Canvas thread. Do not introduce object allocations or heavy operations inside the render loop (`TerminalScreen.kt` or `TerminalBuffer.kt`).

## 2. Development Setup
1. Fork the repository.
2. Clone your fork locally.
3. Open the project in **Android Studio Ladybug** (or newer).
4. Ensure your environment variable `JAVA_HOME` points to JDK 17.
5. Let Gradle sync and resolve dependencies.

## 3. Architecture Rules
*   **MVVM:** All business logic must reside in ViewModels. The Jetpack Compose UI (`/ui/screens/`) should only consume `StateFlow` and dispatch events.
*   **Dependency Injection:** We use **Dagger Hilt**. Do not instantiate repositories or services manually. Inject them.
*   **Coroutines:** Never block the main thread. Network calls (JSch, SFTP) must strictly execute on `Dispatchers.IO`.

## 4. Submitting a Pull Request
1. Create a feature branch from `main`: `git checkout -b feature/your-feature-name`
2. Write clean, Kotlin-idiomatic code.
3. Ensure the project compiles successfully: `./gradlew assembleDebug`
4. Commit with clear, descriptive messages.
5. Push to your fork and submit a Pull Request against our `main` branch.

## 5. Reporting Bugs
If you find a bug (especially in the terminal rendering engine or SFTP transfer service), please open an issue with:
1. Your Android device model and OS version.
2. The exact steps to reproduce the issue.
3. If it's a terminal glitch, specify the CLI tool (`htop`, `vim`, etc.) you were running.
