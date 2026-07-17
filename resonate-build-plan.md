# Resonate — Offline Music Player for Android

**Build spec v1.0.** Written to be handed to Claude Code as the source of truth.

Free. No ads. No trackers. No account. Reads music that already lives on the phone.

---

## 0. The one-line brief

A local-file music player for Android that feels like a piece of well-made hardware: instant, quiet, and skinned by whatever you're listening to. Audience is Gen Z — people who have a 4,000-song folder from ten different places, hate ads, and expect the app to look like it was designed on purpose.

**Non-goals (say no to these, they are what bloat every other player):**
- No streaming, no cloud sync, no "discover" feed
- No login, no social, no scrobbling in v1
- No lyrics scraping in v1 (embedded lyrics only)
- No ads, no analytics SDK, no crash SDK that phones home

---

## 1. Design direction

### 1.1 The signature: chroma bleed

The app is monochrome-dark by default. When a track starts, the dominant colors of its artwork bleed into the UI — the seek bar, the play button, the now-playing background gradient, and the mini-player edge. Change the track, the room changes color. Nothing else in the app is loud, so this is the one thing people remember.

This is not decoration: it is Android's own strength (palette extraction + Material You), it costs one bitmap pass that Coil already does, and it makes the app feel like *your* library rather than a product.

Guardrail: extracted colors are clamped for contrast (WCAG AA on all text) before use. Ugly artwork must not be able to break legibility. Users who hate it get **Settings → Appearance → Chroma bleed: off**, which falls back to a fixed violet accent or the system Material You palette.

### 1.2 Color tokens

Dark is the default and the primary design. Light theme is a port, not the origin.

| Token | Hex | Use |
|---|---|---|
| `ink` | `#0D0B14` | app background |
| `surface` | `#171425` | cards, sheets, bottom bar |
| `surfaceRaised` | `#221D33` | pressed states, dialogs, chips |
| `hairline` | `#2E2842` | 1dp dividers, outlines |
| `bone` | `#EFEAF6` | primary text, icons |
| `muted` | `#9A93AD` | secondary text, timecodes, metadata |
| `pulse` | `#FF5C6E` | accent: play state, active tab, destructive |
| `mint` | `#5BE9C8` | secondary accent: downloads/scan progress, success |

Light theme: `ink → #F6F4FA`, `surface → #FFFFFF`, `bone → #16121F`, `muted → #635C74`, accents unchanged.

`pulse` and `mint` are the *fallback* accents. When chroma bleed is on, the accent slot is filled by the artwork. Deliberate choice: the base palette is desaturated indigo rather than pure black so that extracted colors sit on it without vibrating, and so OLED black is a setting (`Settings → Pure black (AMOLED)`) rather than an aesthetic the user can't escape.

### 1.3 Typography

Three roles, three faces. Bundle the variable fonts locally (~180 KB total after subsetting to Latin + Latin-Ext); do not use Google Fonts at runtime — no network calls.

| Role | Face | Where |
|---|---|---|
| Display | **Bricolage Grotesque** (variable, 600–800) | track title on Now Playing, screen headers, empty states |
| Body | **Figtree** (400/500/600) | list rows, labels, settings, buttons |
| Utility | **JetBrains Mono** (400/500) | timecodes, durations, bitrate/sample rate, track numbers |

Monospace timecodes are functional, not a vibe: proportional digits jitter the seek bar every second. Track duration columns align.

Type scale (sp):

```
display-lg   34 / 38   Bricolage 700, -1.5% tracking   Now Playing title
display-sm   24 / 28   Bricolage 700, -1% tracking     screen headers
title        16 / 22   Figtree 600                     list row primary
body         14 / 20   Figtree 400                     list row secondary
label        13 / 16   Figtree 500                     buttons, tabs, chips
mono         12 / 16   JetBrains 400                   timecodes, technical
caption      11 / 14   Figtree 500, +2% tracking       eyebrows, section labels
```

Never truncate a track title to one line on Now Playing — wrap to two, then marquee only if it still overflows.

### 1.4 Layout & motion

- 4dp base grid. 16dp screen gutters. 12dp corner radius on cards, 999dp on chips and the play button, 28dp on bottom sheets.
- List row height 64dp with a 48dp artwork square. Touch targets never below 48dp.
- One-handed by design: everything you tap while walking lives in the bottom third. The tab bar and mini-player are stacked at the bottom; the Now Playing transport sits at ~70% screen height.
- Motion is limited to three things: the mini-player→Now Playing shared-element expand (400ms, emphasized easing), the transport button squish (scale 0.92, 80ms, plus a haptic tick), and the chroma cross-fade on track change (600ms). Everything else is instant. Respect `Settings.Global.ANIMATOR_DURATION_SCALE` and reduced-motion.
- Edge-to-edge, predictive back, gesture nav insets handled everywhere.

### 1.5 Voice

Plain, active, never cute. Buttons say what happens: "Rescan library," not "Refresh." Empty states are invitations, not apologies:

- Empty library → *"No audio on this device yet. Add files to Music, or pick a folder to scan."* + **Choose folder**
- Search no results → *"Nothing matches "xyz". Try an artist or album name."*
- Permission denied → *"Resonate needs access to your audio files to build your library. Nothing leaves your phone."* + **Grant access**
- Playback error → *"Can't play this file — the format may be unsupported or the file is corrupt."* + **Skip** / **Details**

---

## 2. Screens & navigation

Bottom tab bar with four destinations. Mini-player docks directly above it and persists across all tabs.

```
┌──────────────────────────────┐
│  Header (display-sm)    [⌕]  │
│                              │
│  content                     │
│                              │
├──────────────────────────────┤
│ ▢ track — artist      ⏵  ⏭  │  ← mini-player (64dp)
├──────────────────────────────┤
│  Home   Library  Search  You │  ← tab bar (72dp)
└──────────────────────────────┘
```

### 2.1 Home
- **Jump back in** — horizontal row of last 10 played (artwork-first cards)
- **Recently added** — from `MediaStore.DATE_ADDED`
- **Most played** — from local play counts
- **Shuffle everything** — one big pill, full width
- Nothing algorithmic. Nothing that requires the network.

### 2.2 Library
Sub-tabs (scrollable chip row, remembers last position): **Songs · Albums · Artists · Playlists · Folders · Genres**

- Sort per sub-tab (title / artist / album / date added / duration / play count), asc/desc, persisted per tab
- Fast-scroll rail with letter bubble on Songs/Albums/Artists
- Long-press → multi-select mode (add to queue, add to playlist, share, delete file, details)
- **Folders** is a real filesystem tree — non-negotiable for this audience, since half their library came from downloads and won't have clean tags

### 2.3 Search
- Instant, local, debounced 120ms, matches title/artist/album/folder/filename
- Fuzzy + diacritic-insensitive
- Grouped results with section headers; recent searches when idle
- Filter chips: Songs / Albums / Artists / Folders

### 2.4 Now Playing (expands from mini-player)
- Artwork (square, 12dp radius, chroma source)
- Title (display-lg, 2 lines) · artist · album (tappable → album screen)
- Seek bar: 4dp track, chroma-filled, timecodes in mono at both ends, scrubbable with haptic detents at 25/50/75%
- Transport: shuffle · previous · **play/pause (72dp, chroma-filled)** · next · repeat (off/all/one)
- Secondary row: favorite · lyrics (if embedded) · sleep timer · queue · overflow (equalizer, speed, details, share, set as ringtone, delete)
- Swipe artwork left/right to change track; swipe down to collapse
- Playback speed sheet: 0.5×–2.0×, 0.05 steps, pitch correction toggle

### 2.5 Queue
Bottom sheet from Now Playing. Drag-reorder, swipe-to-remove, "Playing next" section, clear queue, save queue as playlist.

### 2.6 Playlists
Create / rename / delete / reorder. Import & export **M3U8** (critical — lets people move in and out; an app you can leave is an app people trust). Cover = 2×2 mosaic of first four tracks.

### 2.7 You (settings)
- **Appearance** — theme (system/light/dark), pure black AMOLED, chroma bleed on/off, Material You system palette
- **Library** — scanned folders (add/exclude), rescan now, ignore tracks shorter than N seconds (default 30, kills WhatsApp voice notes and ringtones), ignore `.nomedia`, hidden folders
- **Playback** — crossfade (0–12s), gapless, resume on headphone connect, pause on disconnect, skip silence, replay gain (track/album/off), audio focus behavior, mono audio
- **Equalizer** — 5-band + bass boost + virtualizer + presets, plus "Open system equalizer" for devices with a better one
- **Sleep timer** — 5/15/30/60 min, end of track, custom, fade out over last 20s
- **Headset** — single/double/triple click actions
- **Storage** — cache size, clear artwork cache
- **About** — version, open source licenses, link to source

### 2.8 Also ships
- **Home-screen widgets** — 4×1 (compact transport) and 4×2 (artwork + transport), Glance-built
- **Notification / lock screen** — MediaStyle with artwork, seek bar, 5 actions
- **Android Auto** — MediaBrowserService browse tree (v1.1, not v1.0)
- **Bluetooth / wired** — AVRCP metadata, headset button handling
- **Shortcuts** — long-press icon → Shuffle all, Continue listening
- **Share target** — open any audio file from Files/Downloads/WhatsApp in Resonate

---

## 3. Features by priority

### v1.0 — must ship
1. Permission flow (`READ_MEDIA_AUDIO` on 13+, `READ_EXTERNAL_STORAGE` on ≤12) with a real rationale screen
2. MediaStore scan → Room index; incremental rescan via `ContentObserver`
3. Tag reading incl. embedded artwork, disc/track numbers, album artist, year, genre
4. Songs / Albums / Artists / Folders / Genres browsing with sort + fast scroll
5. Playback: play, pause, next, prev, seek, shuffle, repeat (off/all/one), gapless
6. Queue with reorder + save
7. Playlists incl. M3U8 import/export
8. Search
9. Now Playing + mini-player + chroma bleed
10. Notification, lock screen, headset buttons, audio focus, becoming-noisy
11. Sleep timer, playback speed, equalizer
12. Resume state across app death (position, queue, track)
13. Themes, AMOLED, Material You
14. Widget 4×2

### v1.1
Android Auto · crossfade · replay gain · skip silence · embedded lyrics view (LRC + ID3 USLT/SYLT) · tag editor · folder blacklist UI · play count stats page ("your top artists this month," computed locally) · backup/restore of playlists+settings to a JSON file

### v2.0 (only if wanted)
Chromecast · local network (DLNA) · optional Last.fm scrobble · smart playlists (rule-based, local) · desktop-free ID3 fixing via MusicBrainz (opt-in, one explicit network call)

---

## 4. Audio format support

ExoPlayer/Media3 handles nearly all of it natively. Declare intent filters and the scanner whitelist to match.

**Native (no extra work):** MP3, AAC (M4A/MP4/3GP), FLAC, WAV/PCM, Ogg Vorbis, Opus, Matroska audio (MKA), AMR-NB/WB, ALAC, MIDI (device-dependent), AC-3/E-AC-3 (device-dependent)

**Needs the FFmpeg extension (build from Media3 source, +~2–4 MB APK per ABI):** WMA, APE (Monkey's Audio), WavPack, TTA, DSD/DSF, MP2

**Decision:** ship v1.0 without FFmpeg. Keep the APK under ~8 MB. Gate it behind a build flavor (`fullFlavor`) if a user actually asks for APE. Do not carry 4 MB of decoders for formats nobody in this audience owns.

Handle unsupported files gracefully: still index them, show a dimmed row with a "format not supported" badge, and never crash the queue — skip and surface a one-line snackbar.

---

## 5. Tech stack

| Layer | Choice | Why this and not the alternative |
|---|---|---|
| Language | **Kotlin 2.1**, JVM target 17 | — |
| UI | **Jetpack Compose** + Material 3 Expressive | Views would be smaller at runtime but Compose + R8 is close enough now, and it's what Claude Code writes most reliably |
| Min / target SDK | **26 / 36** | 26 covers ~98% of devices and unlocks NotificationChannel + real Java 8 APIs without desugaring pain |
| Playback | **Media3 ExoPlayer** + `MediaSessionService` | The only correct answer on Android. `MediaPlayer` has no gapless, no format breadth, no session plumbing |
| Metadata | MediaStore first; **JAudiotagger (Android fork)** only for the tag editor | MediaStore is already indexed by the OS — reading tags yourself for 4,000 files is the classic 30-second-cold-start mistake |
| Database | **Room** + KSP | Library index, playlists, play counts, queue snapshot |
| Paging | **Paging 3** | Lists must never hold the full library in memory |
| Images | **Coil 3** | Compose-native, and lets you cap the memory cache exactly |
| DI | **Hilt** | Reliable codegen, effectively zero runtime cost. (Koin is lighter to set up but reflection-ish and slower to start.) |
| Async | Coroutines + Flow, `StateFlow` for UI state | — |
| Prefs | **DataStore (Proto)** | SharedPreferences blocks on main thread at load |
| Background | **WorkManager** | Library rescan only |
| Widgets | **Glance** | — |
| Nav | **Navigation Compose** (type-safe routes) | Single activity |
| Palette | **AndroidX Palette** on Coil's decoded bitmap | The chroma bleed source |
| Build | Gradle KTS + version catalog, **R8 full mode**, **Baseline Profile**, resource shrinking, per-ABI splits | — |
| Test | JUnit5 + Turbine + Robolectric; Compose UI tests for the critical flows | — |

**Deliberately absent:** RxJava, Retrofit/OkHttp (no network in v1 — if there's no HTTP client, there can be no telemetry), Firebase, Glide, any ad SDK, any analytics.

### 5.1 Architecture

Single-module to start (`:app`), split later only if build times hurt. MVVM + a real domain boundary:

```
app/
├── di/
├── data/
│   ├── mediastore/     MediaStoreScanner, ContentObserver watcher
│   ├── db/             Room entities, DAOs, migrations
│   ├── prefs/          DataStore
│   └── repo/           LibraryRepository, PlaylistRepository, PlaybackRepository
├── domain/
│   ├── model/          Song, Album, Artist, Folder, Playlist, QueueItem
│   └── usecase/        PlayAlbum, ShuffleAll, ScanLibrary, ...
├── playback/
│   ├── PlaybackService (MediaSessionService)
│   ├── PlayerHolder, QueueManager, SleepTimer, EqualizerController
│   └── notification/
└── ui/
    ├── theme/          tokens, chroma engine, typography
    ├── components/     TrackRow, MiniPlayer, ArtworkImage, FastScroller
    ├── home/ library/ search/ nowplaying/ queue/ playlist/ settings/
    └── widget/
```

**The one rule that matters:** the UI never touches ExoPlayer. UI ↔ `MediaController` ↔ `MediaSessionService` ↔ ExoPlayer. This is what makes the widget, notification, Auto, and Bluetooth all work from one code path, and what makes playback survive the Activity dying.

---

## 6. Performance & memory budget

Targets on a 4 GB mid-range device with a 5,000-track library:

| Metric | Target |
|---|---|
| Cold start → first frame | < 400 ms |
| Full library scan (5k tracks, first run) | < 4 s, off main thread, progress shown |
| Incremental rescan | < 250 ms |
| Idle RSS (browsing) | < 90 MB |
| Playing, screen off | < 60 MB |
| Scroll | 0 dropped frames at 120 Hz |
| APK (per-ABI split) | < 8 MB |

**How:**

1. **Never load the library into memory.** Room + Paging 3, page size 60, prefetch 20. `LazyColumn` with stable `key = song.id` and `contentType`.
2. **Cap Coil hard.** `MemoryCache.Builder().maxSizePercent(context, 0.12)` and a 60 MB disk cache for extracted artwork thumbnails. Request artwork at exactly the display size (`size(48.dp)` for rows) — never let a 3000×3000 embedded JPEG into memory.
3. **Artwork via `ContentResolver.loadThumbnail()`** on API 29+ — the system already has thumbnails, don't re-decode the file. Fall back to `MediaMetadataRetriever` off-thread and cache the result to disk keyed by album ID.
4. **Palette extraction on a 64×64 downscale**, cached in Room by album ID. Extract once per album, never per track.
5. **Scan on `Dispatchers.IO` with a single MediaStore cursor query**, batched into Room in 500-row transactions. Do not open a `MediaMetadataRetriever` per file — that's the difference between 4 seconds and 4 minutes.
6. **ExoPlayer buffer for local playback** is tiny: `DefaultLoadControl` with 5s/10s min/max buffer instead of the streaming default 50s. Saves ~15 MB.
7. **Release the player when the queue ends and no controller is bound.** Stop the foreground service properly.
8. **Baseline Profile** generated for cold start + library scroll + now-playing expand. Free ~30% on first-run jank.
9. **R8 full mode + resource shrinking + `android:extractNativeLibs=false`**, per-ABI APK splits.
10. **No `LiveData`, no `mutableStateListOf` for lists**, use `ImmutableList` from kotlinx.collections.immutable so Compose can skip recomposition.
11. **Compose compiler stability reports on**, in CI. Any unstable parameter in a list row is a bug.
12. **StrictMode on in debug**, thread + VM policy, penalty death for disk reads on main.

---

## 7. Data model

```kotlin
@Entity(indices = [Index("albumId"), Index("artistId"), Index("folderId"), Index("title")])
data class SongEntity(
  @PrimaryKey val id: Long,          // MediaStore _ID
  val uri: String, val title: String, val trackNumber: Int, val discNumber: Int,
  val year: Int, val durationMs: Long, val dateAddedSec: Long, val dateModifiedSec: Long,
  val albumId: Long, val artistId: Long, val folderId: Long, val genreId: Long?,
  val sizeBytes: Long, val mimeType: String, val bitrate: Int?, val sampleRate: Int?,
  val isSupported: Boolean, val relativePath: String, val fileName: String
)

@Entity data class AlbumEntity(
  @PrimaryKey val id: Long, val name: String, val artistId: Long, val artistName: String,
  val year: Int, val songCount: Int,
  val artworkUri: String?, val chromaPrimary: Int?, val chromaSecondary: Int?, val chromaOnColor: Int?
)

@Entity data class ArtistEntity(@PrimaryKey val id: Long, val name: String, val albumCount: Int, val songCount: Int)
@Entity data class FolderEntity(@PrimaryKey val id: Long, val path: String, val name: String, val songCount: Int)
@Entity data class PlaylistEntity(@PrimaryKey(autoGenerate=true) val id: Long, val name: String, val createdAt: Long, val updatedAt: Long)
@Entity(primaryKeys = ["playlistId","position"]) data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val position: Int)
@Entity data class PlayStatEntity(@PrimaryKey val songId: Long, val playCount: Int, val lastPlayedAt: Long, val isFavorite: Boolean)
@Entity data class QueueItemEntity(@PrimaryKey val position: Int, val songId: Long)
```

Play stats and favorites live in their own table keyed by `songId` so a rescan can wipe and rebuild `SongEntity` without ever losing a user's history. **This is the bug every open-source player has shipped at least once.** Reconcile orphans by `uri` on rescan, not by `_ID` (MediaStore reissues IDs).

---

## 8. Build order for Claude Code

Each step ends with the app compiling and runnable. Do not proceed until the previous step runs.

1. **Skeleton** — Gradle KTS, version catalog, Hilt, Compose, single Activity, theme tokens + fonts + typography. Static preview screens.
2. **Permissions + scanner** — rationale screen, MediaStore query, Room insert, progress UI. Prove: 5k tracks in < 4s, log the timing.
3. **Library UI** — Songs tab with Paging 3 + fast scroller. Then Albums/Artists/Folders/Genres. Sorting.
4. **Playback core** — `PlaybackService`, `MediaSessionService`, `MediaController` in UI, play a track from the Songs list. Notification + lock screen. Audio focus + becoming-noisy.
5. **Mini-player + Now Playing** — shared-element expand, transport, seek.
6. **Chroma engine** — Palette on 64×64, cache to Room, contrast clamp, animate.
7. **Queue** — reorder, swipe remove, persist across process death.
8. **Playlists** — CRUD + M3U8 import/export.
9. **Search**.
10. **Settings + equalizer + sleep timer + speed**.
11. **Widget (Glance)**.
12. **Polish pass** — Baseline Profile, R8 full mode, StrictMode audit, memory profile against §6 targets, TalkBack pass, predictive back, RTL.

### Definition of done for every step
- No main-thread I/O (StrictMode clean)
- No `!!`, no `runBlocking` in production code
- Every list row has a stable key and a `contentType`
- Every string in `strings.xml`
- Every touch target ≥ 48dp, every icon has a `contentDescription`
- Compose compiler report shows no unstable params in hot paths

---

## 9. Permissions (the complete list)

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />        <!-- 33+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                 android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" /> <!-- 34+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />      <!-- 33+ -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />  <!-- equalizer -->
```

**No `INTERNET` permission.** Ship it that way and put it in the About screen. For this audience that single line is worth more than any feature — it's a claim the OS itself enforces.

---

## 10. Open decisions to make before coding

1. **App name** — "Resonate" is a placeholder; check Play Store collisions.
2. **Distribution** — Play Store, F-Droid, or GitHub Releases APK? F-Droid forces the no-proprietary-deps discipline that already fits this spec.
3. **License** — GPL-3.0 keeps forks open; MIT lets someone ship it with ads. GPL-3.0 fits the brief.
4. **Fonts** — Bricolage Grotesque and Figtree are both OFL, so bundling is fine. Confirm you like them at 34sp before committing; the display face is 80% of the app's personality.
5. **Min SDK 26 vs 24** — 24 adds ~1.5% of devices and a lot of pain. Recommend 26.
