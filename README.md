# FTP Client for Android (E-Ink Optimized)

A simple, clean FTP client designed for e-ink Android devices. Pure black/white UI, large touch targets, no animations.

## Features
- Connect to any FTP server (FTP over port 21)
- Browse remote directories recursively
- Download files to `/storage/emulated/0/Download`
- Saves last server config automatically
- Passive mode (PASV) toggle
- Anonymous login support

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
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps
1. Open the `FtpClient` folder in Android Studio
2. Wait for Gradle sync to complete
3. Connect your e-ink device via USB (enable Developer Options + USB Debugging)
4. Run → select your device

### Permissions
The app requires:
- `INTERNET` — for FTP connections
- `WRITE_EXTERNAL_STORAGE` (Android ≤ 9) — to save downloads

On Android 10+, downloads go to the public Downloads folder without needing the storage permission.

## Dependencies
- `commons-net:commons-net:3.10.0` — Apache Commons Net (FTP protocol)
- AndroidX ViewModel + LiveData
- Kotlin Coroutines

## Download Directory
Downloads are saved to:
```
/storage/emulated/0/Download/<filename>
```
This is the standard Android public Downloads folder. Files are accessible from any file manager.

## Limitations (v0.1)
- FTP only (no FTPS/SFTP)
- No upload support
- No multi-file selection
- Fixed download directory
- Single active download at a time
- Hardcoded encoding as UTF-8

## Potential Future Features
- FTPS (explicit/implicit TLS)
- File search / filter
- Multiple server bookmarks
- Background download service with notification
- Configurable download directory
- File preview (text files)
