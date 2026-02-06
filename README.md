# XLog GUI

[English](README.md) | [简体中文](README_CN.md)

**XLog GUI** is a modern, high-performance desktop application for viewing and analyzing Tencent Mars XLog files. Built with **Kotlin Compose Multiplatform** and a **Rust** core, it provides a native experience on macOS and Windows with ultra-fast log parsing and decryption capabilities.

## ✨ Features

*   **High Performance**: Powered by a Rust core for parsing and decrypting `.xlog` files with blazing speed.
*   **Cross-Platform**: Native desktop support for macOS and Windows.
*   **Modern UI**: Clean, Material 3 Design interface with "Deep Slate" dark theme.
*   **Log Decryption**: Support for parsing encrypted logs with private keys.
*   **Advanced Filtering**: Filter logs by level (Verbose to Fatal), tags, and processes.
*   **Powerful Search**: Regex-supported search functionality.
*   **File Tree**: Integrated sidebar for easy navigation of log directories.
*   **Export**: Export filtered logs to standard text formats.

## 🛠 Tech Stack

*   **UI**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Kotlin)
*   **Core**: [Rust](https://www.rust-lang.org/) (via JNI) for performance-critical parsing
*   **DI**: [Koin](https://insert-koin.io/)
*   **Build**: Gradle & Cargo

## 🚀 Getting Started

### Prerequisites

*   JDK 17 or higher
*   Rust (Cargo) installed (for building the core)

### Installation

**Download Releases**
Check the [Releases](https://github.com/kexuejin/xlog-gui/releases) page for the latest `.dmg` (macOS) or `.msi` (Windows) installers.

**Build from Source**

1.  **Clone the repository**
    ```bash
    git clone https://github.com/kexuejin/xlog-gui.git
    cd xlog-gui
    ```

2.  **Build the Rust Core**
    ```bash
    make
    # Or manually:
    # cd rust_core && cargo build --release && cp target/release/libxlog_core.dylib ../composeApp/libs/
    ```

3.  **Run the App**
    ```bash
    ./gradlew :composeApp:run
    ```

4.  **Package for Distribution**
    ```bash
    ./gradlew :composeApp:packageDistributionForCurrentOS
    ```

## 📖 Usage

1.  **Open Logs**: Click "Open File" or select a file from the sidebar.
2.  **Decrypt**: If the log is encrypted, click the Lock icon in the toolbar and enter your private key.
3.  **Filter**: Use the dropdown to filter by log level.
4.  **Search**: Type in the search bar to find specific keywords (Regex supported).

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to submit pull requests, report issues, and the code of conduct.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
