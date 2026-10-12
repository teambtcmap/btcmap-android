# AGENTS.md - BTC Map Compose Development Guide

## Project Overview
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Key architectural pillars**: Compose Multiplatform 1.12.1, MapLibre Compose v0.20.0, SQLite

There are four Gradle modules:

- **`:app`** — the Android application: the remaining Views chrome (`AppRootFragment`), platform glue, and the `AppServices`/labels the host needs. It has no Compose compiler.
- **`:shared`** — Kotlin Multiplatform (Android + JVM + wasmJs): the whole portable core (`api`, `db`, `i18n`, `sync`, `openinghours`, `stats`, `settings`, the pure `map` data helpers, `offline`, `payment`, `imagestats`, `auth`/`http`). No Android dependency.
- **`:ui`** — Kotlin Multiplatform (Android + JVM): the Compose Multiplatform UI (`AppTheme`, `MaterialSymbol`, the screens, and the map: `MapScreen`, its layers, markers, chips, search and place sheet), plus the single Android `AbstractComposeView` host (`AppRootComposeView`) in `androidMain`.
- **`:desktopApp`** — the JVM Compose Desktop app, sharing `:shared` and `:ui`.

## Build Commands

Bundled assets (places snapshot, map styles) are managed
outside Gradle via the `./devtools bundle` commands, see below.

## Development Environment (devtools script)

The `./devtools` wrapper manages an Android emulator, website, and app deployment across all supported platforms. Default device is `emulator-5554` running the `Resizable_Experimental` AVD; debug package is `org.btcmap.debug`.

```bash
./devtools emulator start    # Boot AVD asynchronously (logs to emulator.log)
./devtools emulator stop     # Shut down emulator and remove log

./devtools app install       # Build and install debug APK
./devtools app run           # Run the app (desktop by default; --android for the emulator)
./devtools app stop          # Stop the running app (desktop by default; --android for the emulator)
./devtools app uninstall     # Remove debug package from device
./devtools app deploy-beta     # Build and rsync beta APK and desktop packages to btcmap-api server
./devtools app deploy-release  # Build and rsync release APK and desktop packages to btcmap-api server

./devtools bundle data         # Download latest places, areas, comments and events snapshots
./devtools bundle map-styles   # Bundle MapLibre styles, sprites and glyphs
./devtools bundle all          # Run all bundlers

./devtools website deploy      # Build and rsync the documentation site to android.btcmap.org
```

When asked to "launch", "run", or "start" the app, use `./devtools app run` (runs the desktop app, launched detached — see the desktop section below) or `./devtools app run --android` (builds, installs and launches the Android build via monkey). Stop a running app with `./devtools app stop` (desktop) or `./devtools app stop --android` (force-stop on the emulator). Assume the emulator is already running when using `--android`; if it is not, start it yourself with `./devtools emulator start` and wait for boot to complete (check `adb devices` or `adb -s emulator-5554 shell getprop sys.boot_completed`). Use `./devtools app install` when only an install is needed (e.g. before running instrumented tests). `./devtools app deploy-beta` and `./devtools app deploy-release` build and push the APKs and the desktop packages to the remote `btcmap-api` host — use only when explicitly asked to publish a build. Both build the desktop app image, pack it into a Linux AppImage (with `appimagetool`, cached under `${XDG_CACHE_HOME:-~/.cache}/btcmap/appimagetool`) and build the Windows and macOS packages with Conveyor (cached under `${XDG_CACHE_HOME:-~/.cache}/btcmap/conveyor`), uploading everything under `desktop/beta/` and `desktop/release/` respectively: the beta AppImage keeps the stable name `BTC-Map-x86_64.AppImage`, the release one embeds the version, and Conveyor's output is the rest of the directory. Both also purge the files that are overwritten in place — `beta.apk`, `beta-universal.apk`, `latest-app-beta-ver.json`, and every file in the packaged site whose name a rebuild can repeat — from the BunnyCDN `static.btcmap.org` pull zone, so both therefore require a BunnyCDN API key in `BUNNY_API_KEY` or `bunny.api.key` in `local.properties`, and refuse to deploy when neither is set rather than silently leaving stale objects on the CDN. `./devtools website deploy` builds the Hugo documentation site and rsyncs it to `android.btcmap.org` — use only when explicitly asked to publish the site. That site is the Hugo project in `website/`, with its pages in `website/content/` and screenshots in `website/static/images/`.

### Verifying on the emulator

- Reach a signed-in screen by seeding the session into the app DB
  (`adb shell run-as org.btcmap.debug sqlite3 databases/btcmap.db`, keys
  `auth_token` and `user` in the `pref` table), then relaunch.
- Read the screen with `adb shell uiautomator dump /sdcard/ui.xml` + `adb pull`;
  Compose content is visible to it.
- Capture with `adb shell screencap -p /sdcard/x.png` + `adb pull` (piping
  `exec-out screencap -p` mangles the PNG with CRLF). The map's marker colour is
  the app's own palette (teal in the debug app), not a fixed colour.

## Compose Multiplatform

The UI is Compose Multiplatform, shared by Android and desktop.

- A screen is a `@Composable` in `:ui`'s `commonMain`, hosted on Android by an
  `AbstractComposeView` subclass in `:ui`'s `androidMain`: the subclass exposes
  plain `var` properties (backed by `mutableStateOf`) and callbacks, and the
  fragment sets them. `:app` therefore needs no Compose compiler and stays on
  Views. `Content()` wraps the screen in `AppTheme`.
- Screens never touch Android resources: the host resolves strings (often via a
  labels data class) and passes them in.
- Compose content inside an Android `MaterialAlertDialog`: build a `:ui`
  `AbstractComposeView`, set the view-tree lifecycle, saved-state and view-model
  owners on it (a dialog window does not inherit them), then `setView(view)`.
- `AppTheme` (in `:ui`) provides the color scheme (dynamic on Android, system
  light/dark on the JVM), the Material Symbols typeface via `LocalIconFont`, and
  `LocalContentColor` set to `onBackground` (a bare `Text` is otherwise black and
  invisible on a dark background).
- The icon font is the app asset (`app/src/main/assets/material-symbols-*.ttf`);
  the Android-KMP library plugin does not package `androidMain/assets`, so hosts
  pass a `Typeface?` and draw glyphs with the shared `MaterialSymbol` composable.
- Recurring frictions: `internal` does not cross modules (widen to `public`);
  Kotlin will not smart-cast a nullable property from another module (bind a
  local first); the CMP `compose.*` dependency accessors warn but work.

### MapLibre Compose

- `org.maplibre.compose:maplibre-compose` **0.19.0**, Android + desktop. Pin the
  exact version: minor releases contain breaking changes. `:ui` uses
  `jvmToolchain(25)`; the Android target emits JVM 17 bytecode and the JVM target
  25. The desktop app runs on Java 25 with `--enable-native-access=ALL-UNNAMED`.
- The Compose map cannot load the app's `asset://` styles: hosts read the bundled
  style JSON and pass `BaseStyle.Json`, with its sprite/glyph URLs rewritten to
  an `app://` provider (`configureBundledMapResources` / `bundledStyleJson` on
  Android).
- Style images take a `ResolvedStyleImage`; use the `StyleImages.setBitmap`
  helper (`ResolvedStyleImage.fromBitmap`).
- Layer click handlers are a `ClickEvent` extension (`MarkerClickHandler`).
- Offline packs come from `OfflineManager.state`
  (`Loading`/`Ready(packs)`/`Failed`).
- Hiding a marker kind works on 0.19.0 (declare only the included kinds); it did
  not on 0.18.0.

## Web Target (wasmJs)

`:shared` targets Android, JVM and **wasmJs** (compile-only: the browser test
environment needs a Node download the repository policy blocks, and `:ui` has no
web target). The shared core is platform-neutral Kotlin — no `java.*`, OkHttp,
Gson or `Dispatchers.IO` in `commonMain`. It uses Ktor, Okio,
kotlinx.serialization, kotlinx-datetime, atomicfu, and an `org.btcmap.platform`
`expect`/`actual` layer (`ioDispatcher`, `currentLanguage`, `formatInteger`,
`weekdayName`, `PlatformLock`, `commentDateFormatter`; web actuals in
`shared/src/wasmJsMain`).

The data layer is **suspending**, because `androidx.sqlite`'s web
`SQLiteDriver.open` / `SQLiteConnection.prepare` / `SQLiteStatement.step` /
`execSQL` are `suspend` while the JVM/Android ones are synchronous:

- `Database` opens in a separate `suspend fun connect()` step (not the
  constructor); the query objects and `DbStatsReader`/`UserStore` are `suspend`.
- The JVM-only statement-locking wrapper lives in `:shared`'s `jvmMain`/`androidMain`
  behind `openDatabaseConnection`/`withTransaction`; the web returns the driver
  as-is (single-threaded, so no locking).
- `Settings` keeps its reads as an in-memory cache; only `preload`, the session
  writes and the background writer are suspending.

The Compose screens and hosts are still synchronous: `:ui`'s `runDbBlocking`
(JVM `runBlocking`) wraps the direct `db.*` calls, and `:app`/`:desktopApp` use
`runBlocking` for startup `connect`/`preload`. Tests use `runBlocking`.

Deferred until Compose 1.13 (MapLibre Compose has no wasmJs artifact, so `:ui`
cannot target the web):

- Add a `wasmJs` target to `:ui` and a web host, then remove the `runBlocking`
  bridge by moving the screens onto coroutines.
- Replace the neutral web `actual`s (English language, comma grouping, English
  weekday names, ISO comment date) with the browser `Intl` API via JS interop.

## Desktop App (`:desktopApp`)

Shares `:shared` and `:ui`; it opens the shared database under `$BTCMAP_HOME` or
`~/.btcmap`.

```bash
./devtools app run               # open the window (detached; logs to /tmp/btcmap-desktop.log)
./devtools app stop              # stop the running window
./gradlew :desktopApp:test       # Compose UI tests, no window
./gradlew :desktopApp:screenshot -Pscreenshot=settings:/tmp/x.png:dark
```

Launching and stopping the window go through `./devtools`; the Compose UI tests
and the headless `screenshot` renderer have no wrapper and run with Gradle
directly.

- `screenshot` renders one screen headlessly to a PNG; today's screens are
  `settings`, `account`, `report`, `addplace`, `addevent`, `addnote` and
  `payment` (the map needs a real window and GPU, so it is not one of them). The
  third spec field is optional (`dark`/`light`).
- **Never run `screenshot` (or any other process) against the live
  `$BTCMAP_HOME`/`~/.btcmap` while the desktop app is running.** It opens the
  same `btcmap.db`. `Database.initialize` used to delete a database whose
  `user_version` it could not read — a transient lock from the concurrent open
  counted — which wiped the user's session, settings and cached notes. The guard
  is now in (an unreadable version is kept, never deleted), and the `screenshot`
  task forces `BTCMAP_HOME` to a throwaway `build/screenshot-home` so it cannot
  touch live data; override it only with `-PbtcmapHome=<scratch dir>`, never
  `~/.btcmap`.
- When asked to run or restart the window, use `./devtools app run`. It launches
  `:desktopApp:run` detached with its output in `/tmp/btcmap-desktop.log` and
  returns control immediately, so **never run `:desktopApp:run` in the foreground
  or `sleep`/poll waiting for it to start**.
- The running app is identified by `pgrep -f "org.btcmap.desktop.MainK[t]"`
  (the brackets keep the pattern from matching the shell); `./devtools app run`
  checks it and refuses to start a second instance. The log at
  `/tmp/btcmap-desktop.log` is the other marker, and its last lines (e.g.
  `desktop: icon font ...`, and the `Rendered the first map frame` line once the
  map is up) tell you how far startup got when you next look, without blocking on
  it.
- Stop a running window with `./devtools app stop` before a restart so only one
  instance is ever open.
- The running app is an XWayland client (Compose Desktop's Skiko renderer is
  X11-only on Linux), so X11 tools see it even though the session is Wayland.
  Find its window with `xdotool search --name "BTC Map"`; the client window
  carries the app's PID in `_NET_WM_PID` (a separate window owned by
  `mutter-x11-frames` is only the frame and shadow). `xdotool getwindowgeometry
  <id>` gives the client area in physical pixels — add `_NET_FRAME_EXTENTS`'s top
  for the GNOME title bar — and on this 2×-scale display the logical size is
  physical/2. The map logs the same extent as `MapExtent(logical=..., physical=...,
  scale=...)` on the first frame of a render session.
- Capture the window with ImageMagick, which reads the XWayland window directly
  and includes the OpenGL map content: `import -window <id> /tmp/opencode/x.png`
  (client area only, no decorations). This is the reliable path here — `grim`
  fails (GNOME does not implement `wlr-screencopy`) and the GNOME Shell D-Bus
  `ScreenshotArea` rejects the call. A native Wayland window (e.g. Loupe, the
  image viewer) does not appear in the X11 window list, so `import` cannot
  capture it.
- Input **can** be injected into the window here, but only with `ydotool`
  (uinput) — not `xdotool`, and not the GNOME RemoteDesktop portal.
  `xdotool` is X11-only and mutter ignores its XTEST/warp (its pointer reading
  does not track `ydotool`/uinput motion, so it stays frozen), and the
  RemoteDesktop portal (libei) shows an "Allow remote interaction" dialog on
  every session with no persistent grant. `ydotool` writes through
  `/dev/uinput` and never prompts, so it drives the map, buttons and keys
  directly.
  - Prerequisites: the `uinput` kernel module must be loaded and
    `ydotool.service` active (`systemctl --user status ydotool`). Arch's `extra`
    `ydotool` package ships `ydotool`, `ydotoold`, the user unit and
    `80-uinput.rules`. A static `/dev/uinput` node can exist (from the Steam
    `uaccess` rule) with no driver behind it, so opening it returns ENODEV until
    the module is loaded. After a kernel upgrade the running kernel's module tree
    may be gone — reboot so it matches, and persist with
    `/etc/modules-load.d/uinput.conf`. The `uaccess` ACL already grants the user
    access, so adding the user to the `input` group is not needed (it would also
    expose every `/dev/input/event*`).
  - Commands (socket is `$XDG_RUNTIME_DIR/.ydotool_socket`): mouse move
    `ydotool mousemove -- <dx> <dy>` (relative; always use the `--` positional
    form — `ydotool` rejects a single axis, and `-y -1` parses the negative as an
    option), wheel `ydotool mousemove -w -- 0 1` (up, zoom in) / `-w -- 0 -1`
    (down, zoom out), click `ydotool click 0xC0` (left; `0xC1` right, `0xC2`
    middle), keys `ydotool key 28:1 28:0` (Enter), typing into a
    focused field `ydotool type -d 60 -- "text"`. `ydotool mousemove --absolute
    -a` does not move the pointer here (the virtual device is relative-only), so
    position by relative moves from a known origin, below.
  - **Check focus, then position the pointer from a known origin.** The app must
    be the active window (`xdotool getactivewindow` matches its client id), or
    the events land on the desktop and nothing happens.
  - `ydotool` moves in the compositor's **logical** pixels, but
    `xdotool getwindowgeometry`, `import` captures and `xrandr` are in
    **physical** pixels: on this 2×-scale display logical = physical/2 (the same
    factor the map logs as `MapExtent(... scale=2.0)`). A point seen at image
    `(ix, iy)` inside a window whose geometry starts at physical `(wx, wy)` is at
    logical `(wx/2 + ix/2, wy/2 + iy/2)`; the logical screen is `xrandr`'s
    reported size / 2.
  - `xdotool getmouselocation` does **not** track a `ydotool`-driven pointer (the
    uinput device bypasses X, so the reading stays frozen even as the pointer
    moves), so a feedback loop on it cannot work. Instead, flatten the
    acceleration for the session
    (`gsettings set org.gnome.desktop.peripherals.mouse accel-profile flat`,
    restoring the original value afterwards) so moves are 1:1, saturate the
    pointer into the bottom-right corner with repeated large moves
    (`for i in $(seq 1 40); do ydotool mousemove -- 2000 2000; done`), then move
    once by `target - corner` to the logical target. Without the flat profile a
    single large move overshoots wildly.
  - Only the **top** corners are GNOME hotspots (top-left Activities, top-right
    system menu), so clamp into the bottom edge rather than the top. A move alone
    cannot trigger a hotspot; only a stray click can, so do not click until the
    pointer is over the intended target. After the final move, pause ~0.4 s
    before clicking; the first click may only focus/hover, so click again.
  - Verify an event actually reached GNOME with the idle monitor — a mouse or key
    event resets it: `gdbus call --session --dest org.gnome.Mutter.IdleMonitor
    --object-path /org/gnome/Mutter/IdleMonitor/Core --method
    org.gnome.Mutter.IdleMonitor.GetIdletime` (returns ms).

### Cross-platform packaging (Conveyor)

The Windows and macOS packages are built by **Conveyor** (`desktopApp/conveyor.conf`, `dev.hydraulic.conveyor` in `desktopApp`), not by jpackage — jpackage cannot cross-compile, and silently *skips* the formats of a foreign OS, so on Linux only `.deb` (unused) and the AppImage exist. Conveyor produces a Windows MSIX + installer `.exe` + portable `.zip` and an Apple-Silicon `.app` archive with a Sparkle update feed, from any host OS. It is free for OSI-approved licences, which AGPL-3.0 is, and needs the EULA accepted (`--agree-to-license=1`, which `devtools` passes).

- **Versions and inputs come from Gradle.** `writeConveyorConfig` generates `desktopApp/generated.conveyor.conf` (gitignored — it holds absolute paths into this machine's Gradle cache), which `conveyor.conf` includes; `devtools` regenerates it on every deploy. It carries the app version from `app/build.gradle.kts`, the main class, `--enable-native-access=ALL-UNNAMED` and the per-platform inputs. `app.revision` is appended by the build script from the same `versionCode`, because MSIX refuses to install a package whose version fails to increase and a beta is rebuilt many times at one `versionName`.
- **The platform native runtimes are Conveyor configurations.** Skiko's and MapLibre's natives must be in `linuxAmd64`/`windowsAmd64`/`macAarch64` in `:desktopApp`, never on `implementation` or in `:ui`'s `jvmMain`: Conveyor reads the whole runtime classpath as the inputs every package shares, so a native jar declared in a library or on the shared classpath ends up inside the packages for the other platforms too. The plugin adds whichever configuration matches the build machine back to `implementation`, so `run`, the tests and the AppImage still work. `:ui`'s `jvmTest` carries this host's natives because the library itself deliberately carries none.
- **Only Windows x64 and Apple-Silicon macOS are supported.** MapLibre Compose publishes no macOS x64 native runtime (nor does `androidx.sqlite`'s bundled JVM driver), so Intel Macs cannot run the app at all; `app.machines` is restricted accordingly. Linux is still packaged by jpackage as the AppImage.
- **The signing root key is per project, kept out of the repository.** `local.properties` points at it with `conveyor.signing.key` — beside the Android keystore, never in the checkout — and `:desktopApp`'s `writeConveyorConfig` appends an `include` of it to the generated config, so the deploy commands and a manual `conveyor -f desktopApp/conveyor.conf` build both sign with it. It must be backed up: every Windows and macOS package key, the update feeds and the Windows package identity derive from it, so losing or rotating it means already-installed copies can never be updated and Windows treats the app as a different one. `devtools` generates it at that path on first use if the file is missing. A key protected with a passphrase (`conveyor keys passphrase`) needs `CONVEYOR_PASSPHRASE` exported.
- **The packaged Windows build does not start, and that is left to an upstream fix.** Conveyor bakes `-Djava.library.path=<app dir>;<root>\bin` into a packaged launcher, so the system directories a plain JVM has implicitly are gone. MapLibre's Windows runtime then fails to load what it needs — `maplibre-native-c.dll` imports USER32, GDI32, OPENGL32, CRYPT32 and the COM/WinRT layer behind them, and its loader also loads host graphics libraries (`vulkan-1.dll`, `libEGL.dll`, …) by name — and because nothing escapes `main`, the packaged app simply runs with no window rather than reporting anything. Both local workarounds were tried and deliberately reverted instead of being guessed at and shipped: appending the system directories at runtime, from `SystemRoot`, to `java.library.path` and to MapLibre's `org.maplibre.nativeffi.library.dirs`, and setting `java.library.path` through Conveyor's config (which means naming `C:\Windows` literally — Conveyor resolves `${env.*}` while reading the config on the build machine, and its launcher expands no environment variables — and pins the launcher's own directory list). Do not reintroduce either without a Windows machine to verify on; wait for MapLibre Compose or Conveyor to fix it. Until then Windows can still be investigated with `CONVEYOR_DEBUG=1 ./devtools app deploy-beta`, which builds the same packages with `-XX:-DisableAttachMechanism`, a console and `jcmd` enabled (`conveyor-debug.conf`).
- **Uploads never delete from the site.** `conveyor make site` emits only the current version's files and keeps the older ones on the server, because it downloads them to build delta updates; the site directory is also where the channel's base URL points, since Conveyor bakes that URL into the packages and an installed copy only ever checks its own channel.

## Bundled Assets

Places, areas, comments, events and map styles are
committed as assets and refreshed manually with `./devtools bundle`. This is
intentional:

- Never propose adding CI (GitHub Actions or any other pipeline) to this
  repository, including build, test or lint workflows.
- Never propose automating the bundled-asset refresh, adding freshness or
  staleness checks, or treating an out-of-date snapshot as a defect. Refreshing
  the snapshot is a deliberate manual step; the snapshot is only a first-launch
  offline fallback.
- The places, areas, comments and events snapshots carry the full field set the
  app syncs, including each row's real `updated_at` and each area's full
  `geo_json` polygon, so a seeded row is a complete record and the first sync
  only fetches the delta since the snapshot was built. The app is fully usable
  offline on first launch (or while the server is down), and a row that never
  changes is never re-downloaded. Bundling the polygons with the areas snapshot
  is what keeps the community and country chips working offline without a
  separate multi-megabyte geometry download. The events snapshot must send
  `updated_since`, because the endpoint's no-cursor fallback omits
  `updated_at` and so cannot seed a delta cursor.
- The label glyphs the map styles need are bundled by `bundle_map_styles.py`
  (`map-styles/glyphs`). The CJK ideograph and Hangul syllable glyph blocks are
  deliberately not bundled (they are ~90 MB on their own), so labels that fall
  back to them stay online-only.

## Code Style Guidelines

- Source files live under each module's `src/<source set>/kotlin/`, one class per file (filename matches class name); packages mirror directories
- Imports grouped: Kotlin stdlib → `android.*` → `androidx.*` → third-party → project; no wildcard imports
- Extensions preferred over utility classes; live in files named after the extended type (e.g., `FragmentExt.kt`), using receiver type aliases where they help
- Use `org.btcmap.db.Database` for all database access; tables live in `org.btcmap.db.table`
- Read existing table schema and queries before changing or adding; any schema change must include a migration

## Dependencies
- **Networking**: Ktor client (CIO engine on Android/JVM, JS engine on web)
- **JSON**: kotlinx.serialization
- **Date/time**: kotlinx-datetime
- **IO**: Okio
- **Concurrency**: Kotlin Coroutines, plus atomicfu for atomics and a common lock
- **Database**: androidx.sqlite (androidx framework driver on Android, bundled on the JVM, the Web Worker driver on web)
- **Maps**: `org.maplibre.compose:maplibre-compose` (Compose Multiplatform, Android + desktop), which also drives the offline pack downloads
- **Images**: Coil
- **UI**: Compose Multiplatform / Material 3 (Android and desktop); Material Components for the remaining Android Views
- **Async**: Kotlin Coroutines
- **QR Codes**: ZXing core, in shared code
- **Color Picker**: shared Compose picker in `:ui`

## Testing
- Run the tests for the code you changed, and run any test you newly added; do
  not run the whole unit suite unless asked
- Do not run instrumented tests (`connectedDebugAndroidTest`) unless the user
  explicitly asks for them. Never run the full instrumented suite to verify a
  change on your own
- When instrumented tests are run and a MapLibre-based or timing-dependent test
  fails (e.g. `AreaOfflineMapTest`), treat it as an environment issue, not a
  code regression, and do not try to fix it unless explicitly asked
- Compose UI tests that run on the JVM live in `:ui:jvmTest` and `:desktopApp:test`
  (`runComposeUiTest`, no window). `:ui`'s jvmTest puts the app's icon-font
  directory on a `btcmap.iconFontDir` system property, and both test JVMs need
  `--enable-native-access=ALL-UNNAMED`.
- Compose instrumented tests use `createEmptyComposeRule` and
  `androidx.compose.ui.test` (`onNodeWithText`, `onNodeWithTag`, `performClick`,
  `performTextInput`); Espresso cannot see Compose content. Assert on a host
  view's exposed state (e.g. `state`, `sections`, `rows`) where a `RecyclerView`
  adapter used to be read
- Add `testTag` constants in the `:ui` screen for anything a test must drive
  (e.g. `COMMENT_FIELD_TAG`, `BOOST_CONTINUE_TAG`). A desktop screen a test must
  drive is `internal`, not `private`; click a `Switch` through `isToggleable()`
  and a `RadioButton` through `isSelectable()` (a toggle's row text is not
  clickable)

## Commits
- Never create a commit unless the user explicitly asks for one. Implement the
  change and leave it in the working tree for review; the changelog and version
  code rules below apply only once a commit is actually requested.

## Changelog
- Update `CHANGELOG.md` before committing, adding entries under the `## [Unreleased]` section
- Only user-facing or otherwise non-trivial and important changes belong in the changelog

## Version Code
- Bump `versionCode` in `app/build.gradle.kts` with every commit made directly on
  `master` that changes the app: increase it by one and note the new value in the
  commit message (e.g. "Bump the version code to 66")
- Don't bump it for changes that don't affect the shipped app, such as
  documentation, `AGENTS.md` or other metadata
- Do not bump `versionCode` on a feature branch that is not expected to land on
  `master` immediately; the branch can be committed to freely and the bump added
  when its work is merged into `master`
- `versionName` is independent of `versionCode` and is not bumped per commit
