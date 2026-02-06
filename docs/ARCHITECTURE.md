# System Architecture

## Overview

MarsLens is designed with a layered architecture, separating the high-performance native core from the modern reactive UI.

```mermaid
graph TD
    User[User] -->|Interacts| UI[Compose UI Layer]
    
    subgraph "Kotlin/JVM (Desktop)"
        UI -->|ViewModel| VM[ViewModels]
        VM -->|StateFlow| UI
        VM -->|Calls| Repo[Repository/Data Layer]
        Repo -->|JNI| JNI_Bridge[JNI Interface]
    end
    
    subgraph "Native (Rust)"
        JNI_Bridge -->|FFI| RustLib[Rust Core Library]
        RustLib -->|Parses| XLogFile[.xlog File]
        RustLib -->|Decrypts| XLogFile
        RustLib -->|Returns| LogObjects[Log Entries]
    end
```

## Core Workflows

### 1. Log Parsing Flow

```mermaid
sequenceDiagram
    participant UI as UI Layer
    participant VM as MainViewModel
    participant Parser as XlogParser (Kotlin)
    participant Rust as Rust Core
    
    UI->>VM: User selects file
    VM->>Parser: parse(filePath, privateKey)
    Parser->>Rust: decodeXlogNative(path, key)
    
    rect rgb(200, 150, 255)
        note right of Rust: Performance Critical Section
        Rust->>Rust: Open File
        Rust->>Rust: Decompress (Zstd)
        Rust->>Rust: Decrypt (if key provided)
        Rust->>Rust: Parse Binary Format
    end
    
    Rust-->>Parser: Return ArrayList<LogEntry>
    Parser-->>VM: Update State
    VM-->>UI: Render Log List
```

## Directory Structure

*   `composeApp/`: Kotlin Multiplatform code (UI, ViewModels, Data layer).
*   `rust_core/`: Rust library for low-level parsing and decryption.
*   `iosApp/`: iOS specific entry point (Future support).
*   `gradle/`: Build configuration and wrappers.

## Key Technologies

*   **UI**: Jetpack Compose (Multiplatform)
*   **State Management**: ViewModel + StateFlow
*   **Dependency Injection**: Koin
*   **Native Interop**: JNI (Java Native Interface)
*   **Build System**: Gradle (Kotlin) + Cargo (Rust)
