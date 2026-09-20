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
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run a single instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.btcmap.ExampleInstrumentedTest

# Run a single test method
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.btcmap.ExampleInstrumentedTest#useAppContext
```

Instrumented tests run against `emulator-5554`. Treat the emulator as always
available: if `adb devices` does not list it, start it yourself with
`./devtools emulator start` (do not ask the user) and wait for boot to finish
(`adb -s emulator-5554 shell getprop sys.boot_completed` returns `1`) before
running the tests.

`-Pandroid.testInstrumentationRunnerArguments.class` does not reliably run a
comma-separated list: only the first class is executed. To target several
classes, run one `connectedDebugAndroidTest` invocation per class (Gradle reuses
the already-installed APKs, so each run is quick), or run the whole suite with a
plain `./gradlew connectedDebugAndroidTest`.

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

./devtools bundle data         # Download latest places, areas and comments snapshots
./devtools bundle map-styles   # Bundle MapLibre map styles as assets
./devtools bundle all          # Run all bundlers
```

When asked to "launch", "run", or "start" the app, use `./devtools app run` (it builds, installs and launches in one step). Assume the emulator is already running; if it is not, start it yourself with `./devtools emulator start` and wait for boot to complete (check `adb devices` or `adb -s emulator-5554 shell getprop sys.boot_completed`). Use `./devtools app install` when only an install is needed (e.g. before running instrumented tests). `./devtools app deploy-beta` and `./devtools app deploy-release` build and push APK artifacts to the remote `btcmap-api` host — use only when explicitly asked to publish a build.

## Bundled Assets

Places, areas, comments and map styles are committed as assets and refreshed
manually with `./devtools bundle`. This is intentional:

- Never propose adding CI (GitHub Actions or any other pipeline) to this
  repository, including build, test or lint workflows.
- Never propose automating the bundled-asset refresh, adding freshness or
  staleness checks, or treating an out-of-date snapshot as a defect. Refreshing
  the snapshot is a deliberate manual step; the snapshot is only a first-launch
  offline fallback.
- The places, areas and comments snapshots carry the full field set the app
  syncs, including each row's real `updated_at` and each area's full `geo_json`
  polygon, so a seeded row is a complete record and the first sync only fetches
  the delta since the snapshot was built. The app is fully usable offline on
  first launch (or while the server is down), and a row that never changes is
  never re-downloaded. Bundling the polygons with the areas snapshot is what
  keeps the community and country chips working offline without a separate
  multi-megabyte geometry download.

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
- Run both unit tests (app/src/test) and the instrumented tests relevant to the
  change before reporting any task as done; do not treat instrumented tests as
  optional or lower priority
- Assume `emulator-5554` is running. If it is not, start it with
  `./devtools emulator start` and wait for boot before running the tests
- Some instrumented tests are flaky on the emulator and fail independently of
  the change under test. Ignore failures from MapLibre-based map
  rendering/interaction tests (e.g. `MapPlaceSelectionTest`) and other
  timing-dependent render tests: a native crash or a render-timeout assertion is
  an environment issue, not a code regression. Confirm by re-running the test,
  and do not try to fix these flaky tests unless explicitly asked.

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
