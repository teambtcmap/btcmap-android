# AGENTS.md - BTC Map Android Development Guide

## Project Overview
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 37
- **Architecture**: Android Views with ViewBinding, SQLite database, Coroutines for async

## Build Commands

### Full Build & Verification
```bash
./gradlew check              # Run all verification (lint + tests)
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
```

Bundled assets (places snapshot, map styles) are managed outside Gradle via the
`./devtools bundle` commands, see below.

### Running Tests

Unit tests are the default. Run only the tests for the code you touched rather
than the whole suite, and always run a test you newly added to confirm it passes:

```bash
# Run a specific unit test class
./gradlew testDebugUnitTest --tests 'org.btcmap.SyncManagerTest'

# Run all unit tests
./gradlew testDebugUnitTest
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
./devtools bundle map-styles   # Bundle MapLibre map styles as assets
./devtools bundle all          # Run all bundlers

./devtools website deploy      # Build and rsync the documentation site to android.btcmap.org
```

When asked to "launch", "run", or "start" the app, use `./devtools app run` (it builds, installs and launches in one step). Assume the emulator is already running; if it is not, start it yourself with `./devtools emulator start` and wait for boot to complete (check `adb devices` or `adb -s emulator-5554 shell getprop sys.boot_completed`). Use `./devtools app install` when only an install is needed (e.g. before running instrumented tests). `./devtools app deploy-beta` and `./devtools app deploy-release` build and push APK artifacts to the remote `btcmap-api` host — use only when explicitly asked to publish a build. `./devtools website deploy` builds the Hugo documentation site and rsyncs it to `android.btcmap.org` — use only when explicitly asked to publish the site. That site is the Hugo project in `website/`, with its pages in `website/content/` and screenshots in `website/static/images/`.

## Bundled Assets

Places, areas, comments, events and map styles are committed as assets and
refreshed manually with `./devtools bundle`. This is intentional:

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

## Code Style Guidelines

- Source files in `app/src/main/kotlin/`, one class per file (filename matches class name); packages mirror directories
- Imports grouped: Kotlin stdlib → `android.*` → `androidx.*` → third-party → project; no wildcard imports
- Extensions preferred over utility classes; live in files named after the extended type (e.g., `FragmentExt.kt`), using receiver type aliases where they help
- Use `org.btcmap.db.Database` for all database access; tables live in `org.btcmap.db.table`
- Read existing table schema and queries before changing or adding; any schema change must include a migration

## Dependencies
- **Networking**: OkHttp with coroutines extension
- **JSON**: Gson
- **Database**: androidx.sqlite (with framework driver)
- **Maps**: MapLibre (Open-source Mapbox alternative)
- **Images**: Coil
- **UI**: Material Design Components
- **Async**: Kotlin Coroutines
- **QR Codes**: QRGenerator
- **Color Picker**: Colorpicker library

## Testing
- Unit tests (app/src/test) are the default. Run the tests for the code you
  changed, and run any test you newly added; do not run the whole unit suite
  unless asked
- Do not run instrumented tests (`connectedDebugAndroidTest`) unless the user
  explicitly asks for them. Never run the full instrumented suite to verify a
  change on your own
- When instrumented tests are run and a MapLibre-based or timing-dependent test
  fails (e.g. `MapPlaceSelectionTest`), treat it as an environment issue, not a
  code regression, and do not try to fix it unless explicitly asked

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
