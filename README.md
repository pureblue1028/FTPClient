# FTP Client for Android (E-Ink Optimized)

A simple, clean FTP client designed for e-ink Android devices. Pure black/white UI, large touch targets, no animations.

## Features

- Connect to FTP servers with a configurable port (default: 21)
- Browse remote directories, navigate to parent directories, and refresh listings
- Download individual files with progress feedback
- Choose between Downloads, Documents, Music, Pictures, and Movies; remembers the selected directory
- Saves last server config automatically
- Passive mode (PASV) toggle
- Anonymous login support
- Select UTF-8 (default), GBK, or ISO-8859-1 encoding; remembers the selected encoding

## UI Design (E-Ink Considerations)

- Pure black (#000000) on white (#FFFFFF) — no gray gradients
- Window animations disabled to reduce ghosting
- Minimum 64dp row height for reliable touch
- Monospace font for directory/file type labels
- No icons, no images — text-only UI

## Project Structure

```
app/src/main/java/com/ftpclient/
├── model/
│   └── FtpModel.kt          — Data classes (FtpConfig, FtpEntry, etc.)
├── network/
│   └── FtpClientManager.kt  — Apache Commons Net FTP wrapper
├── utils/
│   └── PrefsHelper.kt       — SharedPreferences for saved config
├── viewmodel/
│   ├── LoginViewModel.kt
│   ├── BrowserViewModel.kt
│   └── BrowserViewModelFactory.kt
└── ui/
    ├── LoginActivity.kt      — Server info entry screen
    ├── BrowserActivity.kt    — Directory navigation + download
    ├── FileListAdapter.kt    — RecyclerView adapter
    └── FtpManagerHolder.kt   — Singleton to share FTP connection
```

## How to Build

### Requirements

- JDK 17
- Android SDK Platform 34 and Build Tools 34.0.0
- Gradle 8.4 (provided by the Gradle wrapper)
- Android Studio Hedgehog (2023.1.1) or newer for the IDE workflow
- An Android 8.0+ (API 26+) device or emulator to run the app

### Android Studio

1. Open the repository root (`FTPClient`, containing `settings.gradle`) in Android Studio
2. Wait for Gradle sync to complete
3. Connect your e-ink device via USB (enable Developer Options + USB Debugging)
4. Run → select your device

### Command line

With JDK 17 active and `ANDROID_HOME` pointing to your Android SDK (or `sdk.dir` configured in an untracked `local.properties`), run from the repository root:

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Lint report: `app/build/reports/lint-results-debug.html`

There are currently no automated unit or instrumentation test sources. Building and running Lint do not verify FTP connections or downloads; check those on a device or emulator with an accessible FTP server.

### Permissions and storage compatibility

The manifest declares:

- `INTERNET` — for FTP connections.
- `WRITE_EXTERNAL_STORAGE` — through Android 10 (API 29).
- `READ_EXTERNAL_STORAGE` — through Android 12L (API 32).

Downloads currently use direct filesystem writes to public storage. On Android 6–9, public-directory writes require a runtime storage permission, but the app does not currently request it. On Android 10+, scoped storage restricts these writes; the app does not yet use MediaStore or the Storage Access Framework (SAF). Selecting a directory does not grant access, so downloads may fail with a permission error. These declarations do not guarantee that downloads work on a given Android version.

## Dependencies

- `commons-net:commons-net:3.10.0` — Apache Commons Net (FTP protocol)
- AndroidX ViewModel + LiveData
- Kotlin Coroutines

## Download Directory

The default destination is the device's public Downloads directory, typically:

```text
/storage/emulated/0/Download/<filename>
```

Use the directory button in the browser to select Downloads, Documents, Music, Pictures, or Movies. The app saves this choice for the next session. Arbitrary folders are not supported yet, and write access is subject to the storage limitations above.

## Limitations

- FTP only (no FTPS/SFTP); credentials and file contents travel without transport encryption
- No upload support
- No multi-file selection
- Download destinations are limited to five preset public directories
- Public-directory downloads need Android storage compatibility fixes (see above)
- Single active download at a time

## TODO

These items are planned improvements, not completed features or delivery commitments.

### Storage compatibility

- [ ] Request runtime storage permission on Android 8–9 before writing to public directories.
- [ ] Use MediaStore and/or SAF for downloads on Android 10+.
- [ ] Allow user-selected folders through SAF and persist access permissions where supported.

### Future features

- [ ] FTPS (explicit/implicit TLS)
- [ ] File search / filter
- [ ] Multiple server bookmarks
- [ ] Background download service with notification
- [ ] File preview (text files)
- [ ] Multi-file selection and batch downloads
