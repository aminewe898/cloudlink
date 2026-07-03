# CloudLink Features

CloudLink is packed with developer-first and sysadmin-oriented features designed to make remote server administration frictionless on mobile devices.

### 🌐 Server & Connection Management
- Manage an unlimited number of Linux servers.
- Securely store SSH Passwords or Private Keys (RSA, Ed25519) within the encrypted Android Room database.
- Visual connection status indicators (Online/Offline/Connecting).
- Background SSH multiplexing for simultaneous Telemetry, SFTP, and Shell access.

### 📈 System Telemetry Dashboard
- **Live Graphs**: Custom fluid line charts track CPU usage history in real-time.
- **Hardware Overview**: 
  - RAM consumption and swap status.
  - Disk Space distribution (`/` and attached volumes).
  - Network I/O (Rx/Tx).
- **Service Detection**: Automatically parses `systemctl`, `docker`, `nginx`, `python`, and more to display elegant capability chips representing your server's software stack.

### 🖥️ VT100 Terminal Emulator
- **Full Interactivity**: Not just a log viewer—fully interactive shell.
- **ANSI & VT100 Parsing**: Supports colored output, advanced cursor positioning, and alternate screen buffers.
- **TUI Support**: Run `htop`, `nano`, `vim`, `ncdu` flawlessly.
- **60 FPS Rendering**: Built on a `ConflatedChannel` pipeline to guarantee zero UI lag, even during massive log dumps.
- **Quick-Keys**: Specialized on-screen keyboard row for `ESC`, `TAB`, `Ctrl+C/D/Z`, and Arrow keys.
- **Auto-Keyboard Sync**: Tapping the terminal intuitively slides the UI up to reveal the keyboard without breaking layout.

### 📁 SFTP Manager
- **Desktop-class Navigation**: Browse server directories seamlessly.
- **Full File Operations**: Upload, Download, Copy, Move, Rename, Delete files and folders.
- **File Creation**: Create new directories or empty files directly on the server.
- **Permissions**: Chmod integration for modifying file ownership/access.
- **Foreground Transfer Service**: Download/Upload operations run persistently in an Android Foreground Service, meaning you can minimize the app or lock your phone without interrupting massive file transfers.
- **Progress Tracking**: Track transfer speed and progress right from your notification shade.

### 🎨 Customization
- **Theme Engine**: Instant, non-destructive theme switching.
- **9 Curated Themes**: Includes Modern Dark (Slate), Classic Light, AMOLED, Catppuccin, Dracula, Nord, Tokyo Night, Gruvbox, and Cyberpunk Neon.

### 🛡️ Security Architecture
- **Local-Only**: CloudLink uses zero tracking, zero external telemetry, and does not route your connection through any proxy servers. Your device connects directly to your SSH daemon.
- **Trust-on-First-Use**: SSH Host Key verification is fully supported.
- **Minimal Permissions**: Requests only Internet access and Storage access (for SFTP downloads).
