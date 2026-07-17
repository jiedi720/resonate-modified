# Resonate

**An offline music player for Android that feels like a piece of well-made hardware: instant, quiet, and skinned by whatever you're listening to.**

Free. No ads. No trackers. No account. **No Internet permission** — the OS itself guarantees nothing leaves your phone. Resonate plays the music that already lives on your device.

---

## The signature: chroma bleed

The app is monochrome-dark by default. When a track starts, the dominant colors of its artwork bleed into the UI — the seek bar, the play button, the Now Playing gradient, the mini-player edge, the active tab. Change the track, the room changes color.

Extracted colors are clamped for WCAG AA contrast before use — ugly artwork can never break legibility. Turn it off any time in **You → Appearance**, falling back to the violet accent or your system's Material You palette.

## Features

### Library
- Instant MediaStore scan (25 tracks in 59 ms measured; budget <4 s for 5,000) with live progress
- Songs / Albums / Artists / Playlists / Folders / Genres browsing
- **Folders is a real filesystem tree** — because half your library came from downloads
- Fast-scroll letter rail, per-tab sorting (persisted), paged lists that never load the full library into memory
- Multi-select: long-press → add many songs to queue or playlist
- Excluded folders (with subfolders) via system folder picker
- "Ignore tracks shorter than N seconds" — kills voice notes and ringtones
- Automatic incremental rescan when files change
- **Play stats, favorites, playlists, and queue survive rescans** — reconciled by file, never by database id

### Playback
- Media3 ExoPlayer: MP3, AAC/M4A, FLAC, WAV, Ogg Vorbis, Opus, and more
- Gapless; unsupported files are indexed but dimmed, never crash the queue
- Full media session: notification, lock screen, Bluetooth/AVRCP, headset buttons, audio focus, pause-on-unplug
- Queue: reorder, swipe-to-remove, save as playlist — **persists across app death**, resuming track and position
- Crossfade (0–12 s), skip silence, volume leveling (ReplayGain track/album)
- Resume on headphone/Bluetooth connect
- **Long-audio memory**: mixes and podcasts over 20 minutes resume where you left off
- Sleep timer with 20 s fade, playback speed 0.5×–2.0× with pitch correction
- 5-band equalizer + bass boost + virtualizer + device presets

### Now Playing
- Expands from the mini-player, one-handed transport in the bottom third
- Scrubbable seek bar with haptic detents, mono timecodes
- **Lyrics** — embedded (ID3/Vorbis/M4A) and `.lrc` sidecar files; synced lyrics scroll karaoke-style with tap-to-seek
- Swipe artwork to change track, swipe down to collapse, marquee for long titles

### Home & Search
- Jump back in · Recently added · Most played · Shuffle everything — nothing algorithmic, nothing networked
- Search: instant, debounced, fuzzy, diacritic-insensitive, across title/artist/album/folder/filename

### Your data
- **Your sound**: plays, minutes, top artists and songs this month — computed on-device
- **Backup/restore**: one JSON file with playlists, favorites, history, and settings; survives reinstalls and device moves
- **M3U8 playlist import/export** — an app you can leave is an app you can trust

### System integration
- Home-screen widget (4×2 with artwork, 4×1 compact — resizable)
- App shortcuts: Shuffle all, Continue listening
- Share target: open any audio file from Files/WhatsApp/Downloads
- Themes: system/light/dark, pure black AMOLED, Material You, chroma toggle

---

## Version history

### 1.2.0 — "Your data" *(2026-07-17)*
- **Your sound** stats page: plays, minutes, top artists/songs this month (counted from this version onward)
- **Backup & restore** to a single JSON file (playlists, favorites, play history, all settings)
- Widget gained a compact 4×1 layout (resize it)
- Excluded folders with system folder picker
- Play-event history (auto-pruned after one year)

### 1.1.0 — "Sound quality" *(2026-07-17)*
- Crossfade 0–12 s (fade-through transition)
- Skip silence
- Volume leveling via ReplayGain tags (track/album)
- Resume on headphone/Bluetooth connect
- Long-audio position memory (>20 min files)
- New **You → Playback** settings screen

### 1.0.0 + Wave 1 — "Foundation" *(2026-07-16 → 17)*
- Complete v1.0 per the build spec: scanner, library, playback core, Now Playing, chroma engine, queue persistence, playlists + M3U8, search, settings, equalizer, sleep timer, speed, widget
- Wave 1 additions: lyrics support, real Home data, playback-error snackbar, marquee, multi-select, share target, app shortcuts, play/pause icon morph

---

## Developer guide

### Prerequisites
| Tool | Version | Notes |
|---|---|---|
| JDK | 17 (Temurin) | `JAVA_HOME` must point at it |
| Android SDK | Platform 36, build-tools 36.0.0 | `ANDROID_HOME` or `local.properties` `sdk.dir` |
| Gradle | 8.14.3 | **wrapper only** — never install globally |

No emulator required for building; a device on USB debugging is the test bed.

### Project structure
```
app/src/main/kotlin/com/resonate/player/
├── data/
│   ├── audio/        ReplayGain tag resolver
│   ├── db/           Room entities, DAOs, migrations (v3)
│   ├── lyrics/       LRC parser + embedded-lyrics repository
│   ├── m3u/          M3U8 codec (pure functions)
│   ├── mediastore/   Scanner + ContentObserver watcher
│   ├── prefs/        DataStore (JSON-serialized UserPrefs)
│   └── repo/         Library / Browse / Playback / Playlist / Search / Backup
├── domain/model/     Song, Album, Artist, Folder, FolderTree, Genre
├── playback/         PlaybackService (MediaSessionService), PlayerConnection,
│                     EqualizerController, PlaybackEffectsController
└── ui/               theme (tokens/chroma), components, screens per feature
```

**The one architecture rule:** UI never touches ExoPlayer.
`UI ↔ MediaController (PlayerConnection) ↔ MediaSessionService ↔ ExoPlayer`.
This single path is why the notification, widget, Bluetooth, and lock screen all just work.

### Everyday commands
```powershell
.\gradlew.bat assembleDebug          # build debug APK (per-ABI splits)
.\gradlew.bat testDebugUnitTest      # run unit tests (25 as of 1.2.0)
.\gradlew.bat installDebug           # build + install on the USB device
.\gradlew.bat assembleRelease        # R8 full mode, shrunk, ~3.6 MB/ABI (unsigned)
```
APKs land in `app/build/outputs/apk/<variant>/`.

### Definition of done (every change)
- No main-thread I/O; no `!!`; no `runBlocking` in production code
- Every `LazyColumn` item has a stable `key` and a `contentType`
- Every user-facing string lives in `strings.xml`
- Touch targets ≥ 48 dp; every icon has a `contentDescription`
- Unit-test pure logic (parsers, reconciliation, clamps) — the suite must stay green
- Bump `versionCode`/`versionName` on every user-visible release
- **Never add the INTERNET permission.** It is the product's core promise.

### Database changes
Room schema lives at version 3 (`schemas/` is exported). Any entity change requires:
1. Bump `version` in `ResonateDatabase`
2. Add a `Migration(n, n+1)` in `DataModule`
3. Install over an existing build on-device and check logcat for migration errors

### Release process
1. **One-time**: create a keystore (never commit it):
   ```powershell
   keytool -genkeypair -v -keystore resonate-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias resonate
   ```
2. Add a `signingConfig` to `app/build.gradle.kts` reading credentials from a git-ignored `keystore.properties`
3. `.\gradlew.bat assembleRelease` → signed per-ABI APKs
4. **Switching an installed debug build to release requires uninstall** (different signatures). Export a backup in-app first, reinstall, import.
5. Smoke-test on-device: scan, play, chroma, notification, queue restore
6. Tag the release; distribute via GitHub Releases / F-Droid (spec's recommendation — the no-proprietary-deps discipline already fits)

### Deferred / roadmap
- Tag editor (jaudiotagger is already bundled)
- Shareable stats card image
- Android Auto (media browse tree — the session architecture is ready for it)
- Baseline Profile (needs emulator), true two-player crossfade, StrictMode penalty-death audit
- Genres populate only on Android 11+ (MediaStore limitation)

---

## License

MIT © 2026 Kimhab — see [LICENSE](LICENSE). Free for everyone, forever.

*Built with Jetpack Compose, Media3 ExoPlayer, Room, Coil, Hilt, and Glance. Fonts: Bricolage Grotesque, Figtree, JetBrains Mono (all OFL).*
