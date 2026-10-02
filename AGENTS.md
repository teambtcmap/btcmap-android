# AGENTS.md - BTC Map Android Development Guide

## Project Overview
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 37
- **Architecture**: Compose Multiplatform UI over a shared Kotlin Multiplatform core; on Android the Compose screens are hosted by Views fragments. SQLite database, Coroutines for async. No iOS target.

There are four Gradle modules:

- **`:app`** — the Android application: the remaining Views chrome (fragments), platform glue, and the `AbstractComposeView` hosts for the shared screens. It has no Compose compiler.
- **`:shared`** — Kotlin Multiplatform (Android + JVM): the whole portable core (`api`, `db`, `i18n`, `sync`, `openinghours`, `stats`, `settings`, the pure `map` data helpers, `offline`, `payment`, `imagestats`, `auth`/`http`). No Android dependency.
- **`:ui`** — Kotlin Multiplatform (Android + JVM): the Compose Multiplatform UI (`AppTheme`, `MaterialSymbol`, the screens, and the map: `MapScreen`, its layers, markers, chips, search and place sheet).
- **`:desktopApp`** — the JVM Compose Desktop app, sharing `:shared` and `:ui`.

The only Android-carrying logic left in `:app` outside the UI is `area/AreaFormatting` (`R.string`) and `UserAgent` (`BuildConfig.VERSION_CODE`).

## Build Commands

### Full Build & Verification
```bash
./gradlew check              # Run all verification (lint + tests)
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
```

Bundled assets (places snapshot, map styles) are managed
outside Gradle via the `./devtools bundle` commands, see below.

### Running Tests

Unit tests are the default. Run only the tests for the code you touched rather
than the whole suite, and always run a test you newly added to confirm it passes:

```bash
# Run a specific unit test class
./gradlew testDebugUnitTest --tests 'org.btcmap.sync.SyncManagerTest'

# Run all unit tests
./gradlew testDebugUnitTest
```

`:shared` and `:ui` also have JVM tests, and `:desktopApp` has Compose UI tests
that run without a window:

```bash
./gradlew :shared:jvmTest :ui:jvmTest :desktopApp:test
```

Instrumented tests are **opt-in**: do not run `connectedDebugAndroidTest` on
your own to verify a change, and never run the whole instrumented suite unless
the user explicitly asks for it. When asked, target the single class or method
under discussion:

```bash
# Run a single instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.btcmap.ExampleInstrumentedTest

# Run a single test method
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.btcmap.ExampleInstrumentedTest#useAppContext
```

Instrumented tests run against `emulator-5554`. Only when the user asks to run
them: treat the emulator as always available, and if `adb devices` does not list
it, start it yourself with `./devtools emulator start` (do not ask the user) and
wait for boot to finish (`adb -s emulator-5554 shell getprop sys.boot_completed`
returns `1`) before running the tests.

`-Pandroid.testInstrumentationRunnerArguments.class` does not reliably run a
comma-separated list: only the first class is executed. To target several
classes, run one `connectedDebugAndroidTest` invocation per class (Gradle reuses
the already-installed APKs, so each run is quick).

### Linting
```bash
./gradlew lint               # Run lint analysis
./gradlew lintDebug          # Run lint on debug build
./gradlew lintRelease        # Run lint on release build
```

## Development Environment (devtools script)

The `./devtools` wrapper manages the emulator and app deployment. Default device is `emulator-5554` running the `Resizable_Experimental` AVD; debug package is `org.btcmap.debug`.

```bash
./devtools emulator start    # Boot AVD asynchronously (logs to emulator.log)
./devtools emulator stop     # Shut down emulator and remove log

./devtools app install       # Build and install debug APK
./devtools app run           # Build, install and launch the app via monkey
./devtools app uninstall     # Remove debug package from device
./devtools app deploy-beta     # Build and rsync beta APK to btcmap-api server
./devtools app deploy-release  # Build and rsync release APK to btcmap-api server

./devtools bundle data         # Download latest places, areas, comments and events snapshots
./devtools bundle map-styles   # Bundle MapLibre styles, sprites and glyphs
./devtools bundle all          # Run all bundlers

./devtools website deploy      # Build and rsync the documentation site to android.btcmap.org
```

When asked to "launch", "run", or "start" the app, use `./devtools app run` (it builds, installs and launches in one step). Assume the emulator is already running; if it is not, start it yourself with `./devtools emulator start` and wait for boot to complete (check `adb devices` or `adb -s emulator-5554 shell getprop sys.boot_completed`). Use `./devtools app install` when only an install is needed (e.g. before running instrumented tests). `./devtools app deploy-beta` and `./devtools app deploy-release` build and push APK artifacts to the remote `btcmap-api` host — use only when explicitly asked to publish a build. `deploy-beta` also purges `beta.apk`, `beta-universal.apk` and `latest-app-beta-ver.json` from the BunnyCDN `static.btcmap.org` pull zone, since those files are overwritten in place; it therefore requires a BunnyCDN API key in `BUNNY_API_KEY` or `bunny.api.key` in `local.properties`, and refuses to deploy when neither is set rather than silently leaving stale objects on the CDN. `./devtools website deploy` builds the Hugo documentation site and rsyncs it to `android.btcmap.org` — use only when explicitly asked to publish the site. That site is the Hugo project in `website/`, with its pages in `website/content/` and screenshots in `website/static/images/`.

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

## Desktop App (`:desktopApp`)

Shares `:shared` and `:ui`; it opens the shared database under `$BTCMAP_HOME` or
`~/.btcmap`.

```bash
./gradlew :desktopApp:run        # open the window; launch it in the background, see below
./gradlew :desktopApp:test       # Compose UI tests, no window
./gradlew :desktopApp:screenshot -Pscreenshot=settings:/tmp/x.png:dark
```

- `screenshot` renders one screen headlessly to a PNG; today's screens are
  `settings`, `account`, `report`, `addplace` and `payment` (the map needs a real
  window and GPU, so it is not one of them).
- When asked to run or restart the window, **never run `:desktopApp:run` in the
  foreground and never `sleep`/poll waiting for it to start**. `run` blocks for
  the life of the window, so launch it detached with its output in a named log,
  e.g. `(./gradlew :desktopApp:run > /tmp/btcmap-desktop.log 2>&1 &)`, and return
  control immediately.
- The running app is identified by `pgrep -f "org.btcmap.desktop.MainK[t]"`
  (the brackets keep the pattern from matching the shell). Check it *before*
  launching: if it is already listed, the window is already open — do not start a
  second instance. The log at `/tmp/btcmap-desktop.log` is the other marker, and
  its last lines (e.g. `desktop: icon font ...`, and the `Rendered the first map
  frame` line once the map is up) tell you how far startup got when you next look,
  without blocking on it.
- Stop a running window with
  `pgrep -f "org.btcmap.desktop.MainK[t]" | xargs -r kill` (the brackets keep the
  pattern from matching the shell). Do this before a restart so only one instance
  is ever open.
- A window screenshot captures the whole screen, so focus the window first; the
  third spec field is optional (`dark`/`light`).
- Input cannot be injected into the window here: it is an XWayland client and
  GNOME leaves its pointer virtual, so verify a screen by booting into it with a
  temporary route and screenshotting.

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
- **Networking**: OkHttp with coroutines extension
- **JSON**: Gson
- **Database**: androidx.sqlite (framework driver on Android, bundled on the JVM)
- **Maps**: `org.maplibre.compose:maplibre-compose` (Compose Multiplatform, Android + desktop) and the MapLibre Android SDK for the Views add-place map and the offline-pack downloader
- **Images**: Coil
- **UI**: Compose Multiplatform / Material 3 (Android and desktop); Material Components for the remaining Android Views
- **Async**: Kotlin Coroutines
- **QR Codes**: QRGenerator (Android), ZXing (desktop)
- **Color Picker**: Colorpicker library

## Testing
- Unit tests (app/src/test) are the default. Run the tests for the code you
  changed, and run any test you newly added; do not run the whole unit suite
  unless asked
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
