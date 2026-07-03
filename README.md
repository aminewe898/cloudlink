<div align="center">
<img src="https://github.com/user-attachments/assets/c7096dc6-149d-4adf-8d3a-0d2859e13b79"
     alt="CloudLink Logo"
     width="220"/>

  <h1>CloudLink</h1>
  <p><strong>A modern, high-performance Android application for server administration, telemetry, and secure file transfer.</strong></p>
  
 
  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
  [![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
</div>

<br/>


## 📌 Overview

CloudLink is an enterprise-grade mobile application designed to put your server infrastructure in your pocket. Built purely with **Kotlin** and **Jetpack Compose**, it offers a lightning-fast native experience, replacing multiple standalone apps by combining a VT100 Terminal, an SFTP manager, and a real-time Telemetry dashboard.

## ✨ Key Features

- **High-Performance VT100 Terminal**: Run interactive TUIs (`htop`, `nano`, `vim`) directly from your phone with 60FPS fluid rendering and accurate ANSI sequence parsing.
- **Advanced SFTP Manager**: A desktop-class file browser supporting uploads, downloads, multi-selection, file editing, and background transfers via Foreground Services.
- **Real-Time Telemetry**: Monitor CPU, RAM, Disk, and Network I/O in real-time with fluid graphing capabilities.
- **Enterprise Theming**: Instantly switch between 9 professionally curated themes including Modern Dark, Catppuccin, Nord, and Dracula.
- **Secure by Default**: "Trust on First Use" SSH, encrypted credential storage (Room Database), and key-based authentication (RSA/Ed25519).

> See [FEATURES.md](FEATURES.md) for an exhaustive list of capabilities.

## 📸 Screenshots

<div align="center">
  <img src="docs/screenshots/dashboard.png" width="200"/>
  <img src="docs/screenshots/terminal.png" width="200"/>
  <img src="docs/screenshots/sftp.png" width="200"/>
  <img src="docs/screenshots/telemetry.png" width="200"/>
</div>

## 🏗️ Architecture

CloudLink is built on modern Android development standards:

- **UI Layer**: Jetpack Compose, Material 3
- **Architecture**: MVVM (Model-View-ViewModel), Repository Pattern
- **Concurrency**: Kotlin Coroutines & Flow
- **Dependency Injection**: Dagger Hilt
- **Local Database**: Room (SQLite)
- **SSH/SFTP Protocol**: JSch

See the [Architecture Docs](docs/architecture/README.md) for detailed schematics.

## 🚀 Installation & Build

### Prerequisites
- Android Studio Iguana | 2023.2.1+
- Java JDK 17
- Minimum SDK: Android 8.0 (API 26)

### Clone & Build
```bash
# Clone the repository
git clone git clone https://github.com/aminewe898/cloudlink.git

# Open the project in Android Studio, or build via Gradle
cd CloudLink
./gradlew assembleDebug
```

## 🔒 Security

We take security seriously. All SSH passwords and private keys are stored locally within the encrypted Android Room database and never leave your device. 
Please refer to [SECURITY.md](SECURITY.md) for our vulnerability disclosure process.

## 🤝 Contributing

Contributions are welcome! Whether it's reporting a bug, proposing a new feature, or submitting a Pull Request.
Please read our [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgements

- [JSch](http://www.jcraft.com/jsch/) - Java Secure Channel
- The Android Jetpack Compose Team
- [Catppuccin](https://github.com/catppuccin/catppuccin) (For theme inspiration)
