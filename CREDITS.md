# Credits & Acknowledgements

CloudLink is made possible by the incredible work of the open-source community. We stand on the shoulders of giants.

## Core Dependencies

### [Jetpack Compose](https://developer.android.com/jetpack/compose)
The modern, declarative UI toolkit for Android. CloudLink is built entirely using Compose, leveraging its powerful `Canvas` API for the custom VT100 terminal rendering engine.

### [JSch (Java Secure Channel)](http://www.jcraft.com/jsch/)
A pure Java implementation of SSH2. JSch powers CloudLink's SSH protocol, interactive shell, command execution, and SFTP channels.

### [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
For asynchronous, non-blocking operations. Coroutines ensure that the terminal I/O reader/writer loops run smoothly in the background without dropping frames on the UI thread.

### [Dagger Hilt](https://dagger.dev/hilt/)
The standard dependency injection library for Android, used extensively throughout CloudLink to manage the lifetimes of view models, repositories, and managers.

### [Room](https://developer.android.com/training/data-storage/room)
An abstraction layer over SQLite used for persisting server configurations and connection telemetry logs.

### [AndroidX Security](https://developer.android.com/topic/security/data)
Specifically `EncryptedSharedPreferences`, which provides AES256-GCM encryption with a master key held by Android Keystore. Hardware backing depends on the device.

## Architecture Inspiration
The VT100 Canvas terminal engine was designed from first principles, taking inspiration from the rendering models used in high-performance desktop emulators like Alacritty and the input philosophies of leading mobile clients like Termius and JuiceSSH.
