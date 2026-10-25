# 🗂️ FileSwiper

**A Tinder-style file cleaner for Android.** Swipe right to keep, left to delete, up to postpone — and free up gigabytes of storage in minutes, not hours.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![Build](https://img.shields.io/badge/build-Gradle%208.7-success)

---

## 📖 About

Cleaning a photo gallery is boring — FileSwiper turns it into a game.
Pick what to clean (photos, videos, audio, documents), choose a mode, and swipe through your files like Tinder. Accepted files go to an in-app **Trash** where you review them one last time before permanent deletion.

> ⚡ Nothing is removed from the device until you press **"Delete forever"** in the Trash.

---

## ✨ Features

| | Feature | Description |
|---|---|---|
| 🖼️ | **Four file categories** | Photos · Videos · Audio (`.mp3`, `.wav`) · Documents (`.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx`, `.pdf`, `.txt`) |
| 📅 | **"By months" mode** | Files grouped into a month list; months you've fully cleared get a ~~strike-through~~ mark — visual progress you can see |
| ⚡ | **"Quick mode"** | Shuffles all selected files and serves a random batch of **5–30** items via a slider |
| 👆 | **Swipe gestures** | ➡️ right = **Keep** · ⬅️ left = **Delete** · ⬆️ up (or Skip button) = move the card to the **end of the queue** |
| 🃏 | **Rich cards** | Live thumbnails for photos/videos, colored **Word / Excel / PowerPoint / PDF / TXT** badges for documents, **Play/Pause with a progress bar** for audio |
| 🗑️ | **Trash screen** | Scrollable list with glyphs and sizes, total freed-space counter, and permanent deletion via `MediaStore` (with the system confirmation dialog on Android 11+) |
| ✈️ | **Light animations** | Cards fly away with a gentle fling — low drag thresholds, spring entrance, KEEP/DELETE stamps fade in as you drag |
| 🎨 | **3 themes** | System (default) · Light · Dark |
| 🌐 | **2 languages** | English (default) · Русский — switchable in-app, no restart needed |

---

## 🔄 How the flow works

```
┌──────────────┐     pick categories + mode      ┌──────────────┐     queue empty / back     ┌──────────────┐
│    Setup     │ ──────────────────────────────> │    Swipe     │ ─────────────────────────> │    Trash     │
│  (settings)  │                                 │ session      │                            │ review +     │
└──────────────┘                                 └──────────────┘                            │ delete ∞     │
        ^                                                                                    └──────────────┘
        │                        "Done" returns here                                                  │
        └────────────────────────────────────────────────────────────────────────────────────────────┘
```

1. **Setup** — check the file types, choose *By months* or *Quick mode*, grant storage access when prompted.
2. **Swipe session** — swipe through the queue; the header shows remaining count, month label (monthly mode) and running trash size.
3. **Trash** — review everything you swiped left, see the total size you'd free, then permanently delete. A green success screen shows how much space was reclaimed.

---

## 🛠️ Tech stack

| Layer | Choice |
|---|---|
| Language | **Kotlin 1.9.22** |
| UI | **Jetpack Compose** (BOM 2024.01.00) + **Material 3** |
| Architecture | **MVVM** — `data` / `ui` / `viewmodel` packages |
| DI | Manual DI via `FileSwiperApp` + `AppContainer` (no frameworks) |
| Navigation | `navigation-compose` (setup → swipe → trash) |
| Storage access | `MediaStore` + `ContentResolver` (thumbnails via `loadThumbnail`) |
| Audio | `MediaPlayer` wrapper with play/pause & progress state |
| Persistence | `SharedPreferences` (theme, language, cleared months) |
| min / target SDK | 26 (Android 8.0) / 34 (Android 14) |
| Java toolchain | Java 17 target, Gradle Wrapper **8.7**, AGP 8.2.2 |

### Project structure

```
app/src/main/kotlin/com/danni/fileswiper/
├── FileSwiperApp.kt            # Application: manual DI container
├── MainActivity.kt             # Single activity (AppCompat + Compose)
├── AppRoot.kt                  # NavHost: setup / swipe / trash
├── data/
│   ├── model/Models.kt         # FileItem, MonthBucket, SessionRequest…
│   ├── MediaStoreRepository.kt # scan & delete via MediaStore/ContentResolver
│   ├── audio/AudioPlayer.kt    # MediaPlayer wrapper (play state flow)
│   ├── prefs/SettingsRepository.kt
│   └── storage/StoragePermissions.kt
├── ui/
│   ├── theme/                  # Material3 light + dark palettes
│   ├── common/                 # formatters, shared components, settings dialog
│   ├── components/             # SwipeStack (gesture engine), FileCard, glyphs
│   ├── setup/                  # Setup screen + parts
│   ├── swipe/                  # Swipe session screen
│   └── trash/                  # Trash screen (list + delete forever)
└── viewmodel/
    ├── SetupViewModel.kt       # categories, months, quick mode shuffle
    └── SwipeViewModel.kt       # queue, trash, audio, permanent delete
```

---

## 🔐 Permissions

| Permission | Why |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` | Android 11+: full file access (all media + documents) via "All files access" system toggle |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` | Android 13+: reading media without the broad toggle |
| `READ_EXTERNAL_STORAGE` (≤ Android 12) | Legacy runtime read permission |
| `WRITE_EXTERNAL_STORAGE` (≤ Android 9) | Legacy runtime write permission |

The app checks access on launch and shows a friendly dialog pointing to the right system screen. Deleting media on Android 11+ goes through `MediaStore.createDeleteRequest`, so the **system** shows its own confirmation dialog — no silent deletions.

---

## 🏗️ Building

### Prerequisites
- **JDK 17+** (tested with JDK 21)
- **Android SDK** with platform **android-34** and build-tools **34.0.0**
- A `local.properties` file in the project root with your SDK path:

  ```properties
  sdk.dir=C\:\\Path\\To\\Android\\Sdk
  ```

### Build the debug APK

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

The output lands here:

```
app/build/outputs/apk/debug/app-debug.apk
```

Then install it on a device/emulator:

```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Android Studio
The project is plain Gradle — open the root folder in Android Studio and it will import automatically. (The app was built **without** Android Studio; Gradle Wrapper is all you need.)

---

## 🌍 Localization

Strings live in `app/src/main/res/values/strings.xml` (English) and `values-ru/strings.xml` (Russian). The in-app Settings dialog (⚙️ on the Setup screen) switches language with `AppCompatDelegate` per-app locales — no relaunch required.

---

## 📌 Notes & limitations

- Audio is limited to `.mp3` / `.wav` (as designed), and only files visible to `MediaStore` are scanned.
- A month is marked "cleared" per file-type selection — clearing July with photos+videos selected won't mark July cleared for documents.
- "By months" sessions start from the newest non-cleared month; you can also tap a specific month row to start there.

---

## 📄 License

Released under the [MIT License](LICENSE) — feel free to fork and adapt.

---

<p align="center">Made with ❤️ and Jetpack Compose</p>
