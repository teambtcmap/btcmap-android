# Migration Plan: Compose (Multiplatform) and a Shared Module

Status: Phase 1 (shared module) and Phase 2 (non-map UI to Compose Multiplatform)
are complete — 14 slices, all verified on the emulator. Phase 3 (the map, and the
desktop target) is in progress: the Android spike renders a shared MapLibre
Compose map (see Progress).

## Goal

Add a desktop build eventually. It is **not** pressing, so the plan favours
cheap, independently valuable steps now and defers the expensive, risky work
until the replacement libraries have settled.

Secondary drivers that point the same way:

- Material Components for Android (Views) is in maintenance mode. New Material
  features and platform design updates will arrive on Compose only.
- A shared module also improves the Android build on its own: faster unit
  tests on the JVM and an enforced boundary between logic and UI.

Not a driver: iOS. There is no iOS target planned.

## Decision

**Isolate the logic first. Do not rewrite the UI first.**

The two tracks (shared logic vs. UI) are independent, and coupling them is what
makes migrations like this stall. Concretely:

1. Extract the Android-free logic into a Kotlin Multiplatform `:shared` module
   with an Android target and a JVM target. This is cheap (see below) and
   valuable on its own.
2. Modernise the UI later, **once**, as **Compose Multiplatform** (not plain
   Jetpack Compose), so the Android and desktop UIs — including the map — come
   from one codebase.
3. Migrate the map last. It is the largest and riskiest rewrite, and it depends
   on an Alpha library.

Rewriting the UI to Compose *first* would buy no code sharing, leave the logic
entangled, and then still require the module extraction afterwards — the same
work done in the wrong order.

## Why logic-first is cheap here

Desktop means a **JVM target**, so almost none of the usual multiplatform
library swaps are required:

| Current | Runs on desktop JVM? | Action for a JVM target |
|---|---|---|
| OkHttp (`api/`) | Yes | keep; Ktor only if a Native target appears |
| `java.time` (46 files) | Yes | keep; `kotlinx-datetime` only for Native |
| Gson (31 files) | Yes | keep; `kotlinx.serialization` only for Native |
| `java.io.File` / `InputStream` | Yes | keep |
| `java.util.concurrent` locks | Yes | keep |
| `androidx.sqlite` + drivers | `androidx.sqlite` is multiplatform | no change; the driver is injected (`AndroidSQLiteDriver` in `:app`, `BundledSQLiteDriver` in tests) |

The only real work is removing `android.*` / `androidx.*`: `Context` /
`AssetManager` (bundle seeding) and `Fragment`.

This was verified empirically (see Progress): with only Android and JVM
targets, `commonMain` **accepts `java.*` and JVM-only libraries such as Gson and
OkHttp** (the common metadata compilation is skipped) but **rejects
`android.*`**. So the JVM-bound logic — `java.time`, Gson, OkHttp, `java.io` —
moves to `commonMain` unchanged, with Gson and OkHttp declared as ordinary
`commonMain` dependencies; only the Android references have to be cut.

The SQLite driver does **not** need `expect`/`actual`: `Database(driver, path)`
already injects it, so `:app` passes `AndroidSQLiteDriver` and the JVM tests pass
`BundledSQLiteDriver`.

Caveat: such `commonMain` is not truly portable. Adding a Native or JS target
later would need `kotlinx-datetime` / `kotlinx.serialization` or a JVM-only
intermediate source set, and the common metadata compilation would start
failing on `java.*`. Acceptable while the only targets are Android and desktop.

The codebase is already unusually well set up for this, because dependencies
are injected rather than read from globals:

- `Database(driver: SQLiteDriver, path: String)` — `AndroidSQLiteDriver` is only
  constructed in `App.kt`.
- `SyncManager(seedPlaces, seedEvents, seedComments, seedAreas)` — bundle
  seeding is passed as lambdas, so the manager never sees `Context`.
- `Api(httpClient, baseUrl = {…}, onUnauthorized = {…})` — config is injected.
- `Settings(dbProvider, legacyValues, clearLegacyValues)` — no `Context` in the
  class; settings are backed by the app database, not `SharedPreferences`.

The existing unit tests already run headless on the JVM (`androidx.sqlite.bundled.jvm`,
MockWebServer), so that suite is effectively a prototype of the shared module.

## Current state

Three Gradle modules:

- **`:app`** — the Android application: all remaining Views UI, platform glue,
  and the `AbstractComposeView` hosts for the migrated screens. It has no Compose
  compiler.
- **`:shared`** — Kotlin Multiplatform (Android + JVM): the whole portable core
  (`api`, `db`, `i18n`, `sync`, `openinghours`, `stats`, `settings/Settings`, the
  pure `util` files, the pure `map`/`offline`/`payment`/`imagestats` models, and
  the `auth`/`http` interceptor support). 62 JVM test classes.
- **`:ui`** — Kotlin Multiplatform (Android + JVM): Compose Multiplatform UI
  (`AppTheme`, `MaterialSymbol` and the migrated screens) plus a `:ui:run`
  desktop entry point.

The Android UI is Compose **except for the map**: the still-Views pieces are the
MapLibre `MapView` in `MapFragment` plus the embedded maps in `EventFragment` and
`PlaceFragment`, and `AuthErrorDialogFragment` (a plain message dialog).
`:shared` has no Android dependency; the only Android-carrying logic left in
`:app` is `area/AreaFormatting` (`R.string`) and `UserAgent` (`BuildConfig`).

## Phases

### Phase 1 — `:shared` module and logic extraction

Create `:shared` with an Android target and `jvm()` (the JVM target also gives a
home for JVM tests). Under AGP 9 this means the
`com.android.kotlin.multiplatform.library` plugin and a `kotlin { android {} }`
block — `androidTarget()` is gone and `com.android.library` is incompatible with
the KMP plugin. Add `include(":shared")`; make `:app` depend on it. Move
Android-free packages incrementally, keeping the Android app shippable and
`./gradlew check` green at each step.

Suggested order (least coupled first):

1. `util` (pure files), `openinghours`, `area/AreaFormatting`, `stats`
2. `db/table/*` schema, projections, queries, `db/Database`, `db/StringExt`
3. `sync/*`
4. `api/*`, `i18n/*`, `bundle/JsonReaderExt`
5. pure map data helpers (`MarkerGeoJson`, `EventGeoJson`, caches, layer
   builders that only produce strings)
6. `settings/Settings` with the legacy-prefs import behind `expect`/`actual`

`expect`/`actual` needed for:

- The database path (built from `Context` in `:app`); the driver itself is
  injected, so no `expect`/`actual` is needed for it.
- Legacy `SharedPreferences` import in settings.
- Anything that reaches for `Context` / `AssetManager`.

Stays in `androidMain` / `:app`:

- `bundle/Bundled*` seeding (reads APK assets) — but `SyncManager` is already
  injected, so `sync` itself is shareable.
- All Fragments, Views, adapters, ViewBinding, MapLibre, Coil event listeners,
  QR generation, color picker.

Known friction:

- `internal` visibility does not cross module boundaries: moving code to
  `:shared` forces many declarations `public`.
- Moving ~60 test files alongside the code.
- Keep `androidx.*` out of `commonMain`; compiling `commonMain` against the JVM
  target makes leaks loud.

Expected effort: **1–3 weeks** focused, solo.

### Phase 2 — Non-map UI to Compose Multiplatform

Introduce Compose Multiplatform for the non-map screens, incrementally, behind
the existing navigation. Compose and Views interoperate (`ComposeView` inside a
Fragment; `AndroidView` for still-Views widgets), so screen-by-screen migration
is possible without a big bang.

Reuse the existing ViewModels and `StateFlow`s; they are the seam the logic
extraction already produces.

Screens in scope (~20): auth dialogs, place, report/add place, comments/add
comment, activity feed tabs, event, area, settings (all sub-screens), user
profile, search, saved, boost, payment, stats, dbstats, imagestats.

Expected effort: **2–4 months** focused, solo. Low confidence.

Cheaper alternative: leave the working Android Views UI untouched and build a
**fresh desktop-only CMP UI** over `:shared`. Faster desktop delivery, no risk to
the shipping Android app, at the cost of maintaining two UIs. Only reasonable if
desktop stays a secondary client.

### Phase 3 — Map to MapLibre Compose (and the desktop target)

**Not started.** The last and riskiest phase, gated on `maplibre-compose`
maturing. The map is the app's core and the only part that cannot be shared
without changing rendering stacks; everything else is already shared or platform
glue.

#### Library state (re-verify at implementation time)

- `org.maplibre.compose:maplibre-compose` **0.15.0** (Aug 2026). Android, iOS and
  desktop now share one MapLibre Native (FFI) implementation and one public API;
  the browser uses MapLibre GL JS.
- Stability: **Android/iOS Beta, desktop Alpha**; the API is explicitly unstable
  and minor releases contain breaking changes. **Pin an exact version.**
- **Android:** the MapLibre Android SDK is no longer a transitive dependency;
  add a render runtime (`maplibre-compose-runtime-vulkan-android` or
  `-opengl-android`). The library raised minSdk to 24 (ours is 29). Location
  moved to `location-runtime-gms` / `location-runtime-hms`.
- **Desktop:** requires **Java 25** and `--enable-native-access=ALL-UNNAMED`; the
  app must configure an app-wide cache and provide each Compose window's GPU
  context. (The project's daemon toolchain is Java 21 and the app targets 17, so
  the desktop target needs its own Java 25 toolchain.)
- **Offline:** `org.maplibre.compose.offline` (`rememberOfflineManager`,
  `OfflinePack`, `rememberOfflinePacksSource`) works on Android and desktop.
- Compose compatibility: the library tracks `gradle-compose` / `androidx-compose`
  1.12.0, matching our `compose-plugin` 1.12.1.

#### What has to change

The pure helpers in `:shared` (`MarkerGeoJson`, `EventGeoJson`, `AreaGeometry`,
`MapArea`, the geometry/viewport caches, `OfflineBounds`/`OfflineRegionEstimates`)
stay. What changes is layer construction and the map host:

- `map/layer/*` — today they build `org.maplibre.android.style.layers.*` objects;
  they must be rebuilt on the MapLibre Compose layer/expression DSL.
- `MarkerIcon` and marker images — Android drawables added to the style become
  `ImageBitmap` additions; the Material Symbols font can supply glyphs.
- `MapSetupController`, `MapSelectionController`, `SearchController`,
  `MapStatusBarController`, `BottomSheetController`, `ViewportCache`, and
  `AreasAdapter` (the area chips) — reimplemented on the Compose map.
- `LocationController` → the MapLibre Compose location engine.
- `OfflineMaps` (`OfflineManager` regions) → the Compose `OfflineManager`.
- The map screen becomes a shared composable in `:ui`; the Android
  `MapFragment`/`MapView` and the MapLibre Android SDK dependency are removed, and
  the embedded maps in `EventFragment`/`PlaceFragment` (and the area screen's
  offline controls) use it too.
- `:app` keeps only platform glue: permissions, location services, system bars,
  insets, and the driver/bundle seeding.

Two pieces need their own design:

- **Basemap / offline assets.** The bundled PMTiles archive + `noCompress` +
  `BundledBasemap` + `MapSetupController`'s uncapping of the bundled z0–z4 layers
  have no direct desktop equivalent (no `AssetManager`). Either load the archive
  through a `pmtiles://` source from the desktop cache, or fall back to hosted
  tiles online-only. The `OfflinePacksSource` API can drive an offline-pack
  management UI.
- **Marker icon pipeline.** The app builds marker bitmaps from drawables + the
  icon font at runtime; on Compose those become `ImageBitmap`s added to the style.

#### Desktop target

A new `:desktopApp` (JVM) sharing `:shared` and `:ui`:

- Java 25 toolchain; run with `--enable-native-access=ALL-UNNAMED`.
- A data directory for the database (no `Context`), plus settings and the icon
  font (ship as a resource or download).
- Coil 3 has a desktop backend, so images work; the map needs its GPU context and
  an app-wide cache.
- A shared `App` composable in `:ui` providing navigation and window scaffolding.
- Native packaging (DMG/MSI/DEB) last.

#### Sequencing (each step independently shippable)

1. **Spike.** Add MapLibre Compose to `:ui` and render a map with the existing
   style URI on **Android**, leaving the current `MapFragment` in place. Proves
   the build, the Android render runtime, and CMP/map interop.
2. **Markers and layers.** Port the merchant/event/exchange/boost layer pipeline
   and the marker images.
3. **Controllers and overlays.** Selection, search, bottom sheet, viewport
   caches, and the area chips.
4. **Offline and basemap.** Port `OfflineMaps` to the Compose `OfflineManager`;
   settle the PMTiles strategy for desktop.
5. **Swap the Android map** and delete the MapLibre Android SDK usage.
6. **Desktop entry point** with navigation and the map; then packaging.

#### Risks and unknowns

- **API instability** — expect breakage between minor releases; pin 0.15.x and
  budget for upgrades.
- **Desktop is Alpha** and depends on Compose/Skia internals; the GPU-context and
  cache wiring is new, and Java 25 is a toolchain jump.
- **Feature gaps** — map snapshot isn't supported on any target (our app does not
  use it); "Compose resource URIs" load styles/assets on Android/Web but not
  desktop, which affects how the basemap style is bundled.
- **PMTiles on desktop** and the offline-pack parity are open design questions.

**Effort:** ~1–3 months focused, solo, **very low confidence**; the desktop track
adds more and depends on MapLibre Compose leaving Alpha.

**Recommendation:** start steps 1–2 only when Phase 3 is prioritised, and prefer
waiting for a release where desktop leaves Alpha. The desktop target is not
pressing, so there is no cost to waiting.

## Estimates

Effort for a single experienced developer, focused (not calendar time):

| Phase | Effort | Confidence |
|---|---|---|
| 1. `:shared` + logic | 1–3 weeks | Medium-high |
| 2. Non-map UI → CMP | 2–4 months | Low |
| 3. Map → MapLibre Compose | 1–3 months | Very low |
| **Total** | **~4–8 months** | Low |

Scaling factors:

- Part-time at ~10 h/week: multiply by roughly 3–4 (→ ~1.5–3 years).
- Desktop without an interactive map: drop Phase 3 (→ ~3–5 months). Biggest
  lever on the total.

## Risks

- **MapLibre Compose is Alpha and its API is not stable.** Pin versions and
  expect breaking changes between minor releases; this is why the map rewrite is
  last, and why a non-pressing desktop target is an advantage (wait for it to
  settle).
- **UI-rewrite estimates are notoriously optimistic.** Treat Phase 2 as a range
  with a long right tail.
- **Offline/basemap rework.** The PMTiles archive and MapLibre offline packs are
  Android-specific; desktop needs a different storage/caching path.
- **Visibility churn.** Module extraction forces many `internal` declarations
  public; a large diff with little behaviour change.

## Decisions

Settled before Phase 2:

1. **UI sharing** — the Android UI moves to Compose Multiplatform incrementally,
   screen by screen, so Android and desktop share one UI (not a separate
   desktop-only UI).
2. **Desktop map** — the eventual desktop app needs the interactive map, so
   Phase 3 (MapLibre Compose) stays in scope.
3. No iOS target.
4. No CI, and no automation of the bundled-asset refresh (see `AGENTS.md`).

## Progress

### Phase 1, first slice — done

`./gradlew check` equivalents run: `:shared:build`, `:shared:jvmTest`,
`:app:compileDebugKotlin`, `:app:testDebugUnitTest --tests …PlaceQueriesTest`.

What landed:

- `:shared` module created (`shared/build.gradle.kts`) with an Android target
  and a JVM target.
- Version catalog: added `kotlin-multiplatform` and
  `android-kotlin-multiplatform-library` plugin aliases; both applied
  `apply false` in the root `build.gradle.kts`.
- `settings.gradle.kts`: `include(":shared")`.
- `:app` depends on `project(":shared")`.
- Moved `org/btcmap/search/NameMatch.kt` to
  `shared/src/commonMain/kotlin/...` (made `nameMatchRank` public — `internal`
  does not cross modules) and its test to `shared/src/jvmTest/kotlin/...`.
  `:app` (`SearchController`, `PlaceQueriesTest`) resolves it from `:shared`.

End-to-end check: `./devtools app run` built and installed the debug APK on
`emulator-5554` and launched it; `org.btcmap.debug/org.btcmap.Activity` is the
resumed activity and logcat shows no crash. The `:shared` Android variant is
wired up on device, not just at compile time.

Verified toolchain facts (worth not rediscovering):

- AGP 9.4 + Kotlin 2.4: the shared module must use
  `com.android.kotlin.multiplatform.library`; its block is `kotlin { android {} }`
  (`androidLibrary {}` is deprecated since AGP 9.1).
- `commonMain` accepts `java.*` when every target is JVM-based, but rejects
  `android.*`. See the note above.
- The `:app` Android variant of `:shared` is produced and consumed
  (`:shared:compileAndroidMain` + `bundleAndroidMainClassesToRuntimeJar`).

### Phase 1, second slice — done

Moved `openinghours` (all four files plus `OpeningHoursTest`) and the two pure
`stats` data classes (`StatsEntry`, `StatsSection`) into
`shared/src/commonMain/kotlin/...`; the test went to `shared/src/jvmTest/...`.
`StatsAdapter` and every Fragment stayed in `:app`, and no visibility changes
were needed. Verified with `:shared:jvmTest`, `:app:compileDebugKotlin` and
`:app:compileDebugAndroidTestKotlin` (the androidTest resolves the moved `stats`
types from `:shared` with no extra dependency). No behavior change.

### Phase 1, third slice — done

Moved `db/**` and `i18n/**` (the latter because `db`'s tests use
`getSearchableNames`) into `shared/src/commonMain/kotlin/...`, with the affected
tests in `shared/src/jvmTest/...`. `:shared` gained `androidx.sqlite`, Gson and
OkHttp as `commonMain` dependencies, plus `androidx.sqlite:sqlite-bundled-jvm` and
JUnit for its JVM tests. No SQLite driver `expect`/`actual` was needed — the
driver is injected.

`db`'s only non-multiplatform dependency was `AreaGeometry`, which used the
Android-only `org.maplibre.geojson` parser. It now walks the GeoJSON directly
with Gson and keeps the existing point-in-polygon math, so the whole data layer
is platform-independent. Visibility churn: `AreaGeometry`, `Area.geoJsonGeometry`,
`Event.isWithin` and the four `getSearchableNames` overloads became public
(`:app`'s `SearchController`, `map/` and `area/` use them). Two `:app` call sites
(`Marker.isOutdated` and `PlaceFragment`'s `verifiedAt`/`line`) were rebound to
locals, because Kotlin no longer smart-casts a nullable property across the
module boundary.

Verified: `:shared:jvmTest` (24 test classes), `:app:testDebugUnitTest`,
`:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin`. No behavior
change.

### Phase 1, fourth slice — done

Moved `api/**`, `sync/**`, the portable `util` files (`CoroutineExt`, `DeepLink`,
`InputStreamExt`, `StringExt`, `ThrowableExt`, `ZonedDateTimeExt`) and the
`auth`/`http` support they need (`RequestBuilderExt`, `TokenSettingInterceptor`,
`RateLimitingInterceptor`, `UserAgentSettingInterceptor`) into `:shared`, with
their tests. `:shared` gained `kotlinx-coroutines-core`, `okhttp-brotli` and, for
JVM tests, MockWebServer and `kotlinx-coroutines-test`.

The `settings` seam was avoided rather than abstracted: `ApiHttpClient` now takes
`userAgent`, `token` and `apiUrl` providers as parameters, so `:app`'s `App`
passes `prefs.authToken` and `prefs.apiUrl` and `:shared` never sees `settings`.
`UserAgent` embeds the app's version code, so it was injected too — `Api` carries
a `userAgent` and `UserAgentSettingInterceptor` takes one. Visibility churn:
`SyncController`, `SyncManager`, `SyncEvent`, `SyncState`, `reportSyncFailure`
and `Api.toVerifiedAt` became public (`:app`'s sync observers, `bundle/`).

Verified: `:shared:jvmTest` (50 classes), `:app:testDebugUnitTest` (44 classes),
`:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin`. No behavior
change.

### Phase 1, fifth slice — done

Moved the remaining portable logic into `:shared`: `settings/Settings` (the
portable store; `:app` keeps the `Context`-bound `SettingsExt` — the `prefs`
singleton, `init`, the legacy-`SharedPreferences` import and the
`authToken`/`apiUrl` accessors), `auth/AuthValidation`, `bundle/JsonReaderExt`,
`comment/CommentExt` + `CommentsAdapterItem`, `dbstats`'s
`BundleReader`/`DatabaseFile`/`TableStats`, the `imagestats` models, the pure
`map` data helpers (`AreaGeometryCache`, `EventGeoJson`, `FeatureStore`,
`MapArea`, `MapAreasController`, `MapConstants`, `MarkerGeoJson`,
`StringBuilderExt`), the `offline` state/bounds/estimates/metadata,
`payment/InvoicePaymentPoller`, `place/PlaceExt` + `PlacePhoto` and
`search/SearchAdapterItem`. Visibility churn: the `Settings` members, the
`offline` types, `AuthValidation`, `CommentExt`, `FeatureStore`, `JsonReader`,
`PlaceExt`, `InvoicePaymentPoller` and `MapAreasController`'s geometry-cache
accessors became public.

Verified: `:shared:jvmTest` (62 classes), `:app:testDebugUnitTest` (32 classes),
`:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin`, and the app runs
on the emulator. No behavior change.

## Phase 1 status

Phase 1 is effectively done. Only two files that carry no Android imports still
live in `:app`, and both are platform-bound by design:

- `area/AreaFormatting` maps issue codes to `R.string` resources.
- `UserAgent` embeds `BuildConfig.VERSION_CODE`.

Everything else in `:app` imports `android.*`/`androidx.*`, MapLibre, Coil or
Material — the UI, the map controllers, the asset seeding and the platform glue.
`:shared` now holds the entire portable core.

### Phase 2, first slice — done

Added Compose Multiplatform: a `:ui` Kotlin Multiplatform module (Android + JVM)
applying `org.jetbrains.compose` (1.12.1) and the Compose compiler plugin, with
a Material 3 `StatsScreen` in `commonMain`. `:app` depends on `:ui` and hosts it
through an `AbstractComposeView` (`StatsComposeView`) rather than a
`ComposeView`/`setContent`, so `:app` needs no Compose compiler and can stay on
Views while screens move one at a time.

Piloted on `DbStatsFragment`: its `RecyclerView` + `StatsAdapter` list is now the
Compose `StatsScreen`, with section icons rendered from the app's Material
Symbols typeface (passed in as a `FontFamily`) and hidden from accessibility as
before. The host passes the app's dynamic (Material You) `ColorScheme` into
`StatsScreen`, so the cards follow the DayNight theme instead of always
rendering light.

Verified: `:ui` compiles for Android + JVM; `:shared:jvmTest` (62),
`:app:testDebugUnitTest` (32), `:app:compileDebugKotlin`,
`:app:compileDebugAndroidTestKotlin`; and on the emulator the screen renders its
cards in both light and dark mode with no crash. No behavior change.

### Phase 2, second slice — done

Migrated `ImageStatsFragment` to the same `StatsComposeView`, so both stats
screens render the shared `StatsScreen`. Deleted `StatsAdapter` and the
`stats_section_item`/`stats_row_item` layouts, and updated
`DbStatsFragmentTest` and `ImageStatsFragmentTest` to read the Compose view's
`sections` instead of a `RecyclerView` adapter (the DbStats test had been left
runtime-broken by the first slice). No behavior change.

### Phase 2, third slice — done

Added a shared `AppTheme` in `:ui` (commonMain) that owns the Material 3 color
scheme — `expect`/`actual`: dynamic (Material You) colors on Android, system
light/dark on the JVM — and provides the Material Symbols typeface through a
`LocalIconFont` `CompositionLocal`. `StatsScreen` no longer takes theme or font
parameters; the Android host passes the app's typeface into `AppTheme`.

Added a JVM entry point (`ui/src/jvmMain/.../Main.kt`, `:ui:run`) that renders
the shared `StatsScreen` on desktop, so shared UI can be iterated on without a
device.

Note: the Android-KMP library plugin does not package `androidMain/assets`, so
the icon font stays in the app's assets (one copy shared by the Views code and
Compose) and is supplied through the theme.

### Phase 2, fourth slice — done

Migrated `SettingsFragment` to a shared `SettingsScreen` in `:ui`: a data-driven
`List<SettingsItem>` (`Action` rows and `Toggle` rows) rendered with Material 3
`ListItem`, hosted through a `SettingsComposeView`. The fragment keeps the
Android-only parts — navigation, the map-style and verified-filter dialogs — and
rebuilds the row list when a setting changes.

Added Compose UI test artifacts (`androidx.compose.ui:ui-test-junit4` and
`ui-test-manifest`) and updated the "settings button opens the screen" cases in
`DbStatsFragmentTest` and `ImageStatsFragmentTest` to click the Compose rows
(`createEmptyComposeRule` + `onNodeWithText`), replacing the Espresso
`R.id.dbStatsButton`/`imageStatsButton` lookups.

Verified on the emulator: all eight rows render with their subtitles, the toggle
switches flip and persist, the map-style dialog opens, and the Database row
navigates to the stats screen. No behavior change.

### Phase 2, fifth slice — done

Migrated `ColorSettingsFragment` to a shared `ColorSettingsScreen` in `:ui`:
color rows with a rounded swatch, hosted by a `ColorSettingsComposeView`. The
fragment keeps the third-party color picker and the settings writes, and rebuilds
the rows after a pick. Deleted the now-unused `ColorSwatchView`. No behavior
change.

### Phase 2, sixth slice — done

Migrated `UserProfileFragment` to a shared `UserProfileScreen` in `:ui`:
sectioned account info, saved place and area lists with edit/delete icon actions
(drawn from the Material Symbols font), and a logout button, hosted by a
`UserProfileComposeView`. The fragment keeps navigation, the username/password
dialogs, the sign-out flow and the API calls, and rebuilds the state after a
change. Deleted the now-unused `SavedItemsAdapter` and `saved_item` layout, and
updated the profile instrumented tests (`UserProfileSavedItemsTest`,
`UserProfileErrorHandlingTest`, `UserLogoutTest`, `ChangePasswordRotationTest`)
to drive the Compose view. Also fixed a theme gap this screen exposed:
`MaterialTheme` does not set a content color, so bare `Text` fell back to black
and vanished on a dark background; `AppTheme` now provides the scheme's
`onBackground` as `LocalContentColor`. No behavior change.

### Phase 2, seventh slice — done

Migrated `CommentsFragment` to a shared `CommentsScreen` in `:ui`: a lazy list
of comment rows (person icon, message, date), an empty state, and an add FAB
whose icon is drawn from the Material Symbols font, hosted by a
`CommentsComposeView`. The fragment keeps the view model, the sync/retry logic
and the add-screen navigation. `CommentsAdapter` and its item layout stay for
`PlaceFragment`, which still renders the same rows in Views. Updated
`CommentsFragmentTest` to drive the Compose view. No behavior change.

### Phase 2, eighth slice — done

Migrated `AddCommentFragment`'s form to a shared `AddCommentForm` in `:ui`:
the disclosure, the current fee (with a loading spinner and a retry), a
500-character comment field with a counter and empty-comment validation, and the
continue button, hosted by an `AddCommentFormComposeView`. The invoice/payment
block stays a Views `include`, because `InvoicePaymentController` is shared with
the other payment screens — so this screen is a Compose form plus a Views payment
block for now. Updated `CommentsFragmentTest` to drive the Compose field and
button. No behavior change.

### Phase 2, ninth slice — done

Migrated the shared Lightning invoice block to Compose: an `InvoicePayment`
composable (QR image, pay/copy, start-over) hosted by an
`InvoicePaymentComposeView`. `InvoicePaymentController` now generates the QR
bitmap (Android) and drives the Compose view, keeping the wallet intent, the
clipboard and the start-over confirmation in `:app`. Both consumers (add-comment
and boost) use it, the `invoice_payment` layout is deleted, and the add-comment
and boost payment instrumented tests were updated to drive the Compose block
(the add-comment test's form, left runtime-broken by the previous slice, was
fixed too). No behavior change.

### Phase 2, tenth slice — done

Migrated `BoostFragment`'s form to a shared `BoostForm` in `:ui` (the
disclosure, the duration choices with their quoted prices, and the continue
button), hosted by a `BoostFormComposeView`, so the whole boost screen is
Compose. Removed the now-unused `BoostDuration` button-id mapping (and its unit
test) and updated `BoostPaymentFlowTest` to drive the Compose form. No behavior
change.

### Phase 2, eleventh slice — done

Migrated the activity feed tab (shared by Local and Saved) to a shared
`ActivityFeedScreen` in `:ui`: rows with a Material Symbols icon, place name,
subtitle and relative date, plus the loading, empty and error/retry states,
hosted by an `ActivityFeedComposeView`. The fragment keeps the filter dialog
(chips), the area/interval selection and the item navigation, and builds the rows
(Android resources, plurals and relative time) in a small mapper. Deleted
`ActivityFeedAdapter` and its item layout; retargeted the adapter-diff unit test
to `feedKey()` and updated the feed instrumented tests. The filter dialog is
still Views. No behavior change.

### Phase 2, twelfth slice — done

Migrated the map-style and verified-filter dialogs from their XML radio layouts
to a shared `RadioPickerContent` composable, hosted by a `RadioPickerComposeView`
set into the `MaterialAlertDialog` via `setView`. Deleted
`map_style_dialog.xml` and `verified_filter_dialog.xml`. The activity feed's chip
filter dialog is still Views. No behavior change.

### Phase 2, thirteenth slice — done

Migrated the activity feed's filter dialog to a shared `ChipFilterContent` in
`:ui` (area chips, multi-select; interval chips, single-select), hosted by a
`ChipFilterComposeView` set into the `MaterialAlertDialog`. The fragment keeps
the selection and reloads on each toggle; the dialog stays open until OK.
Deleted `activity_feed_filter_dialog.xml`. This was the last non-auth Views
dialog. No behavior change.

### Phase 2, fourteenth slice — done

Migrated the auth dialogs' content to Compose: the account chooser
(`AuthChooserContent`) and the credential / change-password forms
(`AuthFormContent` — text fields with password visibility toggles, inline errors
and helpers), hosted by `AuthChooserComposeView` / `AuthFormComposeView` set into
the `MaterialAlertDialog` (owner pattern). `AuthFormDialogFragment` now holds the
field values and drives the Compose form; the typed values still live in
`AuthFormViewModel` (retained across rotation, never saved) and results are
delivered through `AuthFormResultViewModel` + `setFragmentResult` as before.
Deleted the `account_choices`, `account`, `account_sign_up` and
`change_password` layouts, and updated the auth instrumented tests
(`AuthDialogValidationTest`, `SignInErrorTest`, `AuthRotationTest`,
`ChangePasswordRotationTest`) to drive the Compose fields.
`AuthErrorDialogFragment` (a plain message dialog) stays Views. No behavior
change.

### Phase 3, first slice — done (Android spike)

Added `org.maplibre.compose:maplibre-compose` **0.18.0** to `:ui`'s `commonMain`
and its OpenGL Android runtime (`maplibre-compose-runtime-opengl-android`), a
shared `MapScreen` composable and a `MapComposeView` host. A temporary
`MapSpikeActivity` in `:app` (manifest entry, launched with `adb shell am start`)
proves rendering.

Verified toolchain facts (worth not rediscovering):

- `:ui` now uses `jvmToolchain(25)` (maplibre-compose's desktop artifact is Java
  25 bytecode); the Android target overrides `jvmTarget` to **17** and the JVM
  target to **25**. AGP 9.4 accepts the JDK 25 toolchain for the Android target.
- On the emulator the OpenGL runtime renders: maplibre-compose logs
  "Rendered the first map frame with OPENGL" and the GPU identifier; the demo
  style shows the expected map colours (pixel-sampled).
- The APK packages both `libmaplibre.so` (the existing Android SDK) and
  maplibre-compose's natives with no duplicate-class conflict, so the two stacks
  coexist during the migration.

`MapSpikeActivity` is throwaway scaffolding; remove it once the shared map
replaces `MapFragment`.

### Phase 3, second slice — done (merchant layers)

Ported `org.btcmap.map.layer.createMerchantLayers` to the MapLibre Compose style
DSL in `ui/src/commonMain/kotlin/org/btcmap/ui/map/MerchantLayers.kt`: a
clustered GeoJSON source plus the cluster background circle, the cluster count
symbol and the marker layer. `MapScreen` now takes the style URL, the markers
and the marker colours; `MapComposeView` exposes them as Compose state; the
spike activity loads real merchants from the database.

Markers draw as circles for now. The Android marker bitmaps (icon glyphs,
comment badges and the boosted/outdated variants) and the event and exchange
pipelines are the following slices.

Verified on the emulator: 126 real merchants loaded and drawn (the brand orange
`#F7931A` accounts for ~11k sampled pixels over the OpenStreetMap "liberty"
style), with no expression errors in logcat.

MapLibre Compose DSL notes worth keeping:

- `neq` and the `convertToString`/`convertToNumber`/`convertToBoolean`
  conversions are **extension functions** (`expr.neq(other)`), not top-level.
- `constStringList` is only the JVM name of a `const(List<String>)` overload.
- `rememberMapState`'s camera seed parameter is `initialCameraPosition`.
- `condition(...)` + `switch(..., fallback = ...)` build the style's `switch`.

### Phase 3, third slice — done (merchant marker bitmaps)

Ported `org.btcmap.map.MarkerIcon`'s merchant marker to Compose graphics in
`ui/.../map/MarkerBitmapFactory.kt`: the same `map_marker` pin outline (as a
`PathParser` path), the Material Symbols glyph measured with `TextMeasurer`, and
the comment badge, at the same dp sizes and variants (boosted, outdated,
`-bN`/`-b9p`). The icon font comes from the host, like the rest of the shared UI.

`MerchantLayers` draws the marker with `iconImage = image(<name expression>)`,
the name matching `org.btcmap.map.merchantMarkerImageName`; `MapScreen`
generates each distinct bitmap and registers it in the style. The pin's alpha
masks and tap hit-testing (`MarkerImageRegistry`) are not ported yet; they belong
with the interaction work.

The important discovery (0.18.0), worth not rediscovering:

- **A layer that references a style image by name only draws it when the image
  already exists in the style *and* the layer is declared after it is added.**
  Neither `MapState.missingImageResolver` nor registering the image afterwards
  makes an already-declared layer draw it. `MapScreen` therefore registers the
  bitmaps first and only then declares the marker layer (re-declaring it for a
  frame when the markers change).
- `image(ImageBitmap)` (the inline overload) is fine on its own — the layer
  registers the image itself — but it cannot vary per feature.
- `StyleImages` has an internal constructor, but `MapState.style.images`
  exposes it, and the commands apply to the loaded style, so wait for
  `MapStyleState.loadState is StyleLoadState.Ready` first.

Verified on the emulator: the pins draw with their glyphs (orange pin, white
shopping-cart and fuel-pump glyphs) over the OpenFreeMap liberty style, and
clustering still works at low zoom.

### Phase 3, fourth slice — done (event and exchange layers)

Ported `createEventLayers` and `createExchangeLayers` to
`ui/.../map/EventLayers.kt` and `ExchangeLayers.kt`: their clustered sources,
the cluster circle/count, the shared plain pin (`MarkerBitmapFactory.pin`) on
the marker layer with the glyph on a second layer
(`MarkerBitmapFactory.icon`), and the exchange comment-count badge circle and
text (with the viewport translate). The marker colors moved into a
`MarkerPalette` data class.

`MapScreen` now takes the events and exchanges too and registers the shared
pin, the event glyph and one glyph per distinct exchange icon; the marker
layers only appear once those images exist, as before. `MapSpikeActivity` loads
merchants, exchanges and events around Warsaw.

Verified on the emulator: 12 merchants, 31 exchanges and 1 event loaded, and
all three render — the merchant glyph baked into the pin, and the exchange and
event glyphs on their own layers over the shared pin.

### Phase 3, fifth slice — done (viewport-driven data)

Ported `org.btcmap.map.ViewportCache`'s camera-driven loading to `:ui`:
`ui/.../map/ViewportBounds.kt` (the expanded viewport and the antimeridian
split, ported from `expand`/`queryByBounds`) and
`ui/.../map/ViewportFeatures.kt`, a `rememberViewportFeatures` composable that
merges what the viewport fetches into the shared `FeatureStore` and publishes
the GeoJSON. `MapScreen` now takes a `Database` and loads merchants, exchanges
and events itself, so `MapSpikeActivity` no longer loads anything.

Notes worth keeping:

- The camera reports `MapEvent.CameraMoveEnded` many times while a gesture is
  still settling (one swipe produced ~38 rounds), so the reload is debounced
  with `collectLatest { delay(200); load() }`; one settled move is one round.
- `MapState.viewport` is nullable, so the initial load waits for
  `StyleLoadState.Ready`, where the viewport first reflects the map size.
- The marker image registration, with the "declare a marker layer only after
  its images exist" rule, still runs whenever the loaded features change.

Verified on the emulator: Warsaw's merchants, exchanges and event load on the
first frame, and panning fetches the new viewport (markers appear and disappear
with the area) in a single query round per settled camera move.

### Phase 3, sixth slice — done (marker selection)

Ported `MapSelectionController`'s tap handling: the marker layers
(`MERCHANT_MARKER_LAYER_ID`, `EXCHANGE_MARKER_LAYER_ID`, `EVENT_MARKER_LAYER_ID`)
now take a `MarkerClickHandler` returning a `ClickResult`, and `MapScreen`
resolves the tapped feature to a place or an event (an event feature has no
`iconId`), loads it with `place`/`event.selectById`, and calls
`onSelectPlace`/`onSelectEvent`. `MapSpikeActivity` shows a toast so the
selection is visible before the bottom sheet exists.

Notes worth keeping:

- MapLibre Compose delivers the tapped features straight to the layer's
  `onClick`, so the manual `queryRenderedFeatures` plus screen-point projection
  is gone, and with it `MarkerImageRegistry`/`AlphaMask` for hit-testing.
- There is no alpha test: a symbol is hit anywhere in its (optionally
  `hitPadding`-expanded) bounds. The masks would only return if a tap had to
  fall through the pin's transparent corners.
- The handler runs on the composition's coroutine scope; the database lookups
  go through `Dispatchers.Default`.

Verified on the emulator: a merchant pin selects place 18048 (`business`), an
exchange pin selects place 13818 (`currency_exchange`), and the event pin
selects event 165.

### Phase 3, seventh slice — done (area chips)

Ported `org.btcmap.map.AreasAdapter`'s chips to `ui/.../map/AreaChips.kt`: the
community and country circles at the map's bottom end, with the API image over
the initials fallback and the upcoming-events badge.
`ui/.../map/MapAreas.kt` drives the shared `MapAreasController` as the camera
settles (the same debounce as the feature loading) and `MapScreen` overlays the
chips. `:ui` now depends on Coil's Compose integration for the chip images,
served on Android by the app's `SingletonImageLoader`.

Verified on the emulator: Warsaw shows the Poland country chip and the
"21 Bitcoin Polska" community chip, each with its event badge, and tapping a
chip selects its area (530).

### Phase 3, eighth slice — done (map search)

Ported `org.btcmap.map.SearchController` and the `SearchView` to the shared UI:
`ui/.../map/MapSearch.kt` (`rememberSearchResults`, the local ranking over
places, areas and events, debounced) and `ui/.../map/SearchOverlay.kt` (the
query field and the results list with the icon, name and distance). `MapScreen`
overlays the search at the top and routes a result to the existing
`onSelectPlace`/`onSelectEvent`/`onSelectArea` callbacks, clearing the query.
The distance formatter is supplied by the host, like the other strings.

Notes worth keeping:

- The geodesic distance is a local haversine (no `android.location` in common
  code), and the search reads the map centre only when a query runs, so it does
  not recompose the map on every camera frame.

Verified on the emulator: typing "bitcoin" lists the ranked local matches with
their icons and distances, and tapping one selects it (event 165) and clears
the field.

### Phase 3, ninth slice — done (place sheet, stage 1)

The first stage of the `PlaceFragment` bottom sheet:
`ui/.../PlaceSheet.kt` renders a `ModalBottomSheet` with the place name, the
companion warning, the verification state, the contact rows (address, phone,
website, email, social, opening hours) and the Verify/Report/Boost/Comments and
Add comment actions. `PlaceAction` and `PlaceSheetStrings` keep `:ui` free of
the app's resources, and `MapScreen` opens the sheet when a place is selected
(from a marker or a search result).

Still to come from `PlaceFragment`: the photo list and upload, the preview map,
the inline comment list, the toolbar overflow (view/edit on OSM, save), and the
verification/outdated warnings.

Verified on the emulator: selecting place 18048 ("Swag42") opens the sheet with
its details and actions, and tapping Verify reports the action back.

### Phase 3, tenth slice — done (place sheet, stage 2: comments)

`PlaceSheet` now shows the place's comments: a "Comments (N)" header and the
rows (`CommentRow`, extracted from `CommentsScreen` so both share it).
`MapScreen` loads the comments when the selected place changes, mapping them
through the shared `Comment.toAdapterItem` and the app's localized date
formatter.

Verified on the emulator: selecting "Bishops Brew" shows its two comments with
their dates below the action row.

### Phase 3, eleventh slice — done (place sheet, stage 3: overflow menu)

`PlaceSheet` now has the toolbar overflow next to the place name, with
Directions, Share, View on btcmap.org and — when the place has an OSM id —
View/Edit on openstreetmap.org (the URLs come from the shared
`Place.osmUrl`/`osmEditUrl`). The items raise the new `PlaceAction`s, which the
host handles. The bookmark/save item is not ported: it needs an authorized
session and the saved-places API.

Verified on the emulator: opening "Swag42"'s sheet shows the overflow with all
five items, and choosing View on OSM reports the action back.

### Phase 3, twelfth slice — done (place sheet, stage 4: verification warning)

The verification row in `PlaceSheet` is now tappable when the place is not
verified or is outdated, and opens the "Verification needed" dialog with the
matching explanation and an OK button. `MapStatusBarController` is left out on
purpose: its whole job is the window insets, so it stays platform glue in
`:app` rather than moving to `:ui`.

Verified on the emulator: "Swag42" (last verified Feb 2024) opens the dialog.

### Phase 3, thirteenth slice — done (place sheet, stage 5: preview map)

`PlaceSheet` takes an optional preview-map slot and `MapScreen` fills it with
`PlacePreviewMap`: a small, non-interactive `MaplibreMap` (`MapInteractions.None`,
`MapUiOptions.None`) centred on the place and drawing its marker with the same
bitmap factory as the main map. `MapUiOptions.None` does not hide the
attribution or the logo in 0.18.0 (`PlatformUiOptions` exposes no flags in common
code), so the preview shows the standard attribution bar for now.

Verified on the emulator: opening "Swag42" shows its preview map with the marker.

### Phase 3, fourteenth slice — done (offline packs, step 4)

Ported `org.btcmap.offline.OfflineMaps` to the Compose offline manager as
`ui/.../map/OfflinePacks.kt`. It uses `DefaultMapRuntime.instance.offlineManager`
and maps each pack's `DownloadProgress` (`Healthy`, `Error`, `TileLimitExceeded`,
`Unknown`) onto the shared `OfflineAreaState`, so the existing mapping and its
tests carry over. A download replaces any pack the area already has, so a
re-download cannot orphan a region on disk.

Verified on the emulator: a small pack for area 530 downloads from 0% to
`Complete(bytes = 67,883,857, maxZoom = 12, styleUrl = liberty)`, survives an app
restart (the persisted pack is listed and reported complete), and the
"replace an existing pack" path runs before the re-download.

The PMTiles/basemap side of step 4 is not touched yet: the bundled archive is
Android-only (`AssetManager`), and the desktop strategy is still open.

### Phase 3, fifteenth slice — done (step 5: the map's area chips)

The map screen now draws its chips with the shared composable: `AreaChipsView`
(`:ui` androidMain) hosts `AreaChips`, `map_fragment.xml` swaps the `RecyclerView`
for it, and `MapFragment` feeds it the areas, the app's colors and the click,
keeping the current list for the activity-feed button. `AreasAdapter` and
`area_item.xml` are deleted, and `AreaInitialsTest` moves to `:ui`'s new
`jvmTest` source set (the first test in `:ui`).

Known gap: the connectivity-driven `AreasAdapter.refreshImages()` has no
equivalent — Coil's `AsyncImage` retries when the composable re-runs, not on a
connectivity change.

Verified in the app itself (not the spike): the chips render on the Views map
screen and a chip tap opens the area screen.

### Phase 3, sixteenth slice — done (bundled styles for the Compose map)

The Compose map can now use the app's bundled styles. `:ui`'s androidMain adds
`configureBundledMapResources(context)`, which registers an `app://` resource
provider serving the bundled sprites and glyphs from the assets, and
`bundledStyleJson(context, assetPath)`, which reads a bundled style and rewrites
its `asset://map-styles/` URLs to that scheme. `MapScreen` and
`PlacePreviewMap` take `styleJson` and use `BaseStyle.Json` when it is set;
`App` configures the provider once at startup.

This was the blocker found in the previous slice: the Compose map cannot read
`asset://` styles, so it drew only the background.

Verified on the emulator: the spike renders the bundled **liberty** style with
its tiles, labels, sprites and glyphs (no glyph or style load errors), and the
place sheet's preview map renders the same bundled style.

### Phase 3, seventeenth slice — done (step 5: the place preview map)

The place screen's embedded `MapView` is gone: `place_fragment.xml` now uses the
new `PlacePreviewMapView` (`:ui` androidMain), which hosts the shared
`PlacePreviewMap`. `PlacePreviewMap` takes the coordinates and an optional
marker (the screen can show coordinates before the place has synced), and the
style; the host resolves the bundled style JSON from the style URL. The
fragment now only sets the view's properties, and `PlaceFragment` no longer
imports any MapLibre Android SDK class.

Two details worth keeping:

- The preview map must not be composed until the style URL is known: a map
  created with an empty style stays stuck on the style background.
- The tap-to-open behaviour (a tap on the non-interactive preview opened the
  place on the map) is an `onClick` on the host view now; the old touch-slop
  handling existed only to tell a tap from a scroll in the nested scroll view.

Verified in the app: the standalone place screen (activity feed → a place)
renders the bundled dark-matter style with the place's marker.

### Phase 3, eighteenth slice — done (step 5: the event map)

The event screen's embedded `MapView` is gone: `event_fragment.xml` uses the new
`EventPreviewMapView` (`:ui` androidMain), which hosts the shared
`EventPreviewMap`. It draws the event marker with the shared pin and glyph
images and the bundled style, and the host's `zoomIn`/`zoomOut` drive the shared
camera through the `MapState` it is handed. `EventFragment` no longer imports
any MapLibre Android SDK class.

Verified in the app: the `btcmap.org/event/165` deep link opens the event
screen, which renders the bundled dark-matter style with the event marker, and
the zoom buttons zoom the shared map.

### Phase 3, nineteenth slice — done (step 5: the map search)

The map screen's `SearchBar`/`SearchView` and its results adapter are gone:
`map_fragment.xml` now uses the new `SearchOverlayView` (`:ui` androidMain),
which hosts the shared `SearchOverlay`. The overlay gained the map menu's
actions (`SearchActions`: add a place, open the settings) and a placeholder, and
`MapFragment` feeds it the query (debounced before `SearchController` runs), the
results and the row click. `SearchAdapter`, `search_adapter_item.xml` and its
diff test are deleted; the instrumented search tests now click the Compose rows.

Verified in the app: typing a place name lists it with its distance, and tapping
the row clears the field and opens the place sheet.

### Phase 3, twentieth slice — done (place sheet: photos and bookmark)

`PlaceSheet` gained the two pieces the `PlaceFragment` sheet still had: an
**Add photo** button and a photo strip (a `LazyRow` of thumbnails, `AsyncImage`,
shown when there are photos), and a **Save** item at the top of the overflow
whose bookmark glyph reflects the saved state. `MapScreen` takes the photos and
the saved flag from the host and reports the selected place
(`onPlaceSelected`), so the host can fetch them; the spike fetches a place's
images through the shared API.

The photo *viewer/pager* is still to port; the strip itself is verified below.

Verified on the emulator: the sheet shows the Add photo button, the overflow
shows Save (bookmark glyph) above Directions/Share/View/Edit, and the photo
strip renders — "Chit Hole Phuket Brewery" (which has six images) shows its
thumbnails above the Add photo button.

### Phase 3, twenty-first slice — done (place photo viewer)

Tapping a thumbnail opens `PlacePhotoViewer`: a full-screen dialog paging
through the place's photos with a close button, ported from
`PlacePhotoViewerDialogFragment`. The sheet owns the viewer's state, so no host
plumbing was needed.

Verified on the emulator with "Chit Hole Phuket Brewery": tapping a thumbnail
opens the photo full screen, a swipe pages to the next one, and the close button
returns to the sheet.

### Main map swap — blocked (assessed)

`MapFragment` has ~46 references to the Views map and three features a swap
would regress, so the main map cannot be swapped in one slice yet:

- **Filter** (the merchants/events/exchanges buttons) shows one marker kind by
  emptying the other two sources — exactly the case the recorded 0.18.0 quirk
  breaks on the Compose map. **Now confirmed experimentally**: hiding a kind by
  keeping its layers and giving the hidden ones a valid never-matching filter
  (`["==", "x", "y"]`) stops the *visible* kind drawing too, just like taking
  the layers out or emptying the source. So no in-place hiding works in 0.18.0.
  The way around it is to change the map's *shape* rather than hide anything:
  either key the map state on the filter so the map is rebuilt with only the
  selected kind declared, or feed one kind-tagged source and let each kind's
  layers filter on the tag (untested). Both still need trying.
  **The rebuild was tried and did not work either**: keying the map state on the
  filter and declaring only the selected kind (a single `MerchantLayers`) drew
  no markers at all, even though the map itself rendered. That is unexplained —
  it may be the keying, or something about a lone layer set — but it means the
  filter is not solved, and the main map swap stays blocked.
- **Location** — `LocationController` is not ported; the Compose location API and
  the permission flow still have to be wired.
- **Offline/basemap** — the offline toggle and `BundledBasemapStyle`'s layer
  uncapping have no Compose equivalent yet.

### Phase 3, twenty-second slice — done (step 6: the desktop module)

A new `:desktopApp` JVM module (Kotlin JVM + Compose Desktop, toolchain 25,
`--enable-native-access=ALL-UNNAMED`, packaged as DMG/MSI/DEB) renders the shared
`StatsScreen`, so the module and the shared UI run end to end on the desktop.
The Kotlin plugins are applied without versions because they are already on the
build classpath, and the `run` task needs `javaHome` pointed at the Java 25
toolchain — it otherwise launches the Gradle daemon's JVM and fails with
`UnsupportedClassVersionError` (the app is class file 69).

Verified: `:desktopApp:run` opens the window and stays up (killed by the test's
timeout, no exception); the Android modules are unaffected, so the version code
is unchanged. Navigation, the database and the map on desktop follow.

### Phase 3, twenty-third slice — done (step 6: desktop data and settings)

`:desktopApp` now opens the shared `Database` on the JVM: a per-user data
directory (`$BTCMAP_HOME` or `~/.btcmap`), the bundled SQLite driver, and
`Settings` over the database with no legacy values to import. The window renders
the shared stats screen with the counts it reads back.

Verified: running the app creates `~/.btcmap/btcmap.db` with the full schema at
version 107, and the window stays up. The icon font is not loaded on desktop
yet, so the stats cards pass no icon.

### Phase 3, twenty-fourth slice — done (step 6: the desktop icon font)

The desktop app now loads the Material Symbols typeface and passes it to
`AppTheme`, so icons render as ligatures instead of raw text. While running from
the sources the font comes from the Android asset directory, which Gradle hands
over as `-Dbtcmap.iconFontDir` (`BTCMAP_ICON_FONT` overrides it, and the newest
`material-symbols-*` file wins, so the versioned name does not have to be kept in
sync). Packaging the font into the desktop distribution is still to do.

Verified as far as this environment allows: the run logs the font it loaded
(`.../app/src/main/assets/material-symbols-outlined-2026-08-28.ttf`) with no
error. The desktop window cannot be screenshotted here (`import` fails on this
X setup), so the rendering itself was not seen.

### Phase 3, twenty-fifth slice — done (step 6: the map on desktop)

The desktop app now renders the shared map. The pieces the desktop needs beyond
the UI:

- **`MapRuntimeOptions(cacheFile = ...)`** pointing at a file under the data
  directory — a *file*, not a directory: a directory path makes the offline
  manager fail to open its database.
- **`kotlinx-coroutines-swing`**, for `Dispatchers.Main` on the AWT event thread;
  without it the map runtime cannot be created.
- **`rememberAwtComposeMapPresentationHost(window)` + `ProvideMapPresentationHost`**
  around the map, so each window supplies its GPU context.
- The style is the hosted `tiles.openfreemap.org/styles/liberty` URL: the bundled
  styles are Android assets and their sprite/glyph provider is Android-only.

Verified by the run log: `Rendered the first map frame with OPENGL on
maplibre-linux-map-renderer, extent 480x702`. The window still cannot be
screenshotted here.

### Phase 3, twenty-sixth slice — done (step 6: sync and data on the desktop)

The desktop app now syncs the shared cache: it builds the shared `Api` (OkHttp
with the same interceptors, `api.btcmap.org`, a desktop user agent) and a
`SyncManager`, starts a sync on launch and offers a Sync button, showing the
state and the place count over the map.

The desktop has no bundled snapshot to seed from (the seed importers read
Android assets), so the no-op seeds make the first sync pull the whole delta
history. That took about a minute and left 42,981 places, 1,008 areas, 2,843
comments and 172 events in `~/.btcmap/btcmap.db`, so the map and the shared
screens have real data.

### Phase 3, twenty-seventh slice — done (step 6: desktop navigation)

The desktop window gained a `NavigationRail` with a **Map** and a **Cache**
screen; the cache screen shows the sync state, a Sync button and the shared
stats screen (recomputed when the sync state changes), and the map keeps the
whole window otherwise.

Verified visually: the rail renders with Map selected beside the shared map,
its markers and the area chips.

### Taking a desktop screenshot in this environment

The session is GNOME on Wayland and neither `import` (ImageMagick here has no
X11 delegate) nor `grim` (mutter does not implement wlr-screencopy) works, and
`org.gnome.Shell.Screenshot` is denied. The portal does work:

```bash
gdbus call --session --dest org.freedesktop.portal.Desktop \
  --object-path /org/freedesktop/portal/desktop \
  --method org.freedesktop.portal.Screenshot.Screenshot "" '{}'
# writes ~/Pictures/Screenshot.png (a prompt may appear)
```

Launch the app first so its window is focused — the portal captures the whole
screen, so whatever is in front is what lands in the file.

### Phase 3, twenty-eighth slice — done (desktop settings screen + headless screenshots)

Two things:

- The settings every host shares (`apiUrl`, `authToken`/`authorized`,
  `showAttribution`, `mapRotationEnabled`) moved from `:app` to
  `shared/.../settings/AppSettings.kt`; only the resource-dependent ones (colors,
  style names) stay with their host. The package is unchanged, so the Android app
  is untouched.
- The desktop window gained a **Settings** screen (API URL, Show attribution and
  Map rotation toggles, the account state), and `:desktopApp` gained a
  **`screenshot` task** that renders a screen to a PNG without a window
  (`ImageComposeScene`): `./gradlew :desktopApp:screenshot -Pscreenshot=<screen>:<path>`
  (`settings` and `cache` today). The map needs a real window and GPU context, so
  it is not one of the screens this can draw. This makes the desktop UI verifiable
  headlessly, which the window screenshots could not do.

Verified: the headless render of the settings screen shows the API URL, both
toggles and the account row.

### Phase 3, twenty-ninth slice — done (desktop window background)

The deskop window's own background is white and Compose Desktop draws nothing
behind a screen that does not paint one; `SettingsScreen`'s `ListItem`s are
transparent, so the light window showed through a dark theme. The window (and the
headless render) now wrap the content in a `Surface`, which paints the theme's
background. `AppTheme` also gained a `darkTheme: Boolean?` override, and the
screenshot spec accepts a third field (`dark`/`light`) — the headless render has
no system theme to read, so it always rendered light before.

Still to do on desktop: the map style is the hosted light `liberty` one, so the
map stays light in dark mode; it should follow the style setting.

### Phase 3, thirtieth slice — done (desktop map style)

`MapStyle`, its preference mapping and `Settings.mapStyle` moved from `:app` to
`:shared` (same package, so Android is untouched); the Context-dependent
`uri`/`name`/`offlineStyleUrl` stay with Android. A new `MapStyle.hostedStyleUrl`
resolves the style a host without bundled assets should load, and the desktop map
follows the system's dark mode through it.

Verified in the window: the desktop map is now the **dark** hosted style in this
dark session, with its markers, cluster and area chips.

### Phase 3, thirty-first slice — done (step 6: a distributable)

`:desktopApp` copies the icon font out of the Android assets into its resources
under a fixed name at build time (no 10 MB duplicate in the repository), and
loads it from there first, falling back to the asset directory when running from
the sources. `createDistributable` then produces a self-contained app image
(`build/compose/binaries/main/app/BTC Map`), with the JVM runtime and the font
inside it.

Verified: the packaged launcher runs on its own — `desktop: icon font
material-symbols.ttf (bundled)` and `Rendered the first map frame …` with no
project directories in play. The map styles are still fetched from the network
(the hosted URLs); bundling them (and the PMTiles basemap) is not done. `packageDeb`
needs `dpkg-deb`, which this machine does not have, so only the app image was
produced here.

### Phase 3, thirty-second slice — done (desktop activity feed)

The desktop shell gained a **Feed** screen: it asks the shared
`MapAreasController` for the areas around the starting point, fetches the shared
`Api.getActivity` for them and renders the shared `ActivityFeedScreen` with rows
it maps itself (icons, per-type subtitles and a relative date). The app's own row
mapping stays in `:app` because it needs Android plurals.

Verified in the window: the feed lists a real recent entry ("Adrian GSM Fix —
Added by Comino, 2 days ago").

## Working notes

Durable facts and conventions for continuing the migration.

### MapLibre Compose 0.18.0 layer quirks

- **The app's styles are `asset://map-styles/<style>/style.json`** (a MapLibre
  Android SDK scheme). The Compose map cannot load them: the load fails and only
  the style background draws. Every embedded map, and the main map swap, is
  blocked on feeding the Compose map the bundled style another way — most likely
  reading the JSON and passing `BaseStyle.Json`, with its sprite/glyph URLs
  rewritten — which is the step 4 basemap piece. (Tried swapping the place
  screen's preview `MapView` to the shared preview map: it compiled and laid out
  but stayed blank for exactly this reason, so it was reverted.)
- Taking a marker kind's layers out of the declaration (an early return, or an
  `if`) makes the other marker kinds stop drawing entirely. So does setting the
  layer's own `visible` property, and so does emptying the hidden kind's source
  features. The map screen therefore keeps all three kinds declared with real
  data, and the show/hide toggle from the map's button group is not ported yet.
- A never-true filter is not an escape hatch either: `const(false)` is rejected
  as "filter value must be a non empty array" and `const(1).eq(const(0))` as
  "filter property must be a string", both aborting the style.

### Modules and hosting pattern

- `:app` (Android application), `:shared` (KMP android+jvm, portable logic),
  `:ui` (KMP android+jvm, Compose UI).
- A migrated screen is a `@Composable` in `:ui`'s `commonMain` plus an
  `AbstractComposeView` subclass in `:ui`'s `androidMain`. The subclass exposes
  plain `var` properties (backed by `mutableStateOf`) and callbacks; the fragment
  sets them. `:app` therefore needs no Compose compiler and stays on Views.
  `Content()` wraps the screen in `AppTheme`.
- Screens never touch Android resources: the fragment resolves strings (often via
  a labels data class) and passes them in, so `:ui` stays resource-free.
- Compose content inside an Android `MaterialAlertDialog` (the map-style and
  verified-filter pickers): build a `:ui` `AbstractComposeView`, set the
  view-tree lifecycle, saved-state and view-model owners on it (a dialog window
  does not inherit them), then `setView(view)`. A selection applies the setting
  and dismisses.

### Theme, icons, Compose gotchas

- `AppTheme` (in `:ui`) provides the color scheme — dynamic
  (`dynamicLight/DarkColorScheme`) on Android, light/dark on the JVM — the
  Material Symbols typeface via `LocalIconFont`, and `LocalContentColor` set to
  `onBackground` (`MaterialTheme` alone leaves it black, invisible on a dark
  background).
- The icon font is the app's asset (`app/src/main/assets/material-symbols-*.ttf`).
  The Android-KMP library plugin does **not** package `androidMain/assets`, so it
  cannot move into `:ui`. Hosts take a `Typeface?` and convert it to a
  `FontFamily`; draw icons with the shared `MaterialSymbol` composable, which
  hides the ligature from accessibility.
- Recurring frictions: `internal` does not cross modules (widen to `public`);
  Kotlin will not smart-cast a nullable property from another module (bind a local
  first); the CMP `compose.*` dependency accessors emit build-script deprecation
  warnings but work.
- Migrating a screen usually retires its Views: delete the adapter and item
  layout it used, and retarget the tests that read them. The only Views left are
  the map itself (see Phase 3) and `AuthErrorDialogFragment`.

### Tests

- Instrumented tests that touch a migrated screen use `createEmptyComposeRule`
  and `androidx.compose.ui.test` (`onNodeWithText`, `onNodeWithTag`,
  `performClick`, `performTextInput`); Espresso cannot see Compose content. Assert
  on the host view's exposed state (e.g. `state`, `sections`, `emptyMessage`,
  `qr`, `rows`) where a `RecyclerView` adapter used to be read.
- Add `testTag` constants in the `:ui` screen for anything a test must drive
  (existing: `COMMENT_FIELD_TAG`, `COMMENT_CONTINUE_TAG`, `BOOST_CONTINUE_TAG`,
  `BOOST_OPTION_TAG_PREFIX`, `FEED_RETRY_TAG`).
- Instrumented tests are compile-checked but **not run** here (`AGENTS.md`); keep
  them compiling and honest anyway.

### Process (every slice)

1. Implement in `:ui` and wire the `:app` fragment; keep behavior identical.
2. Verify: `:shared:jvmTest`, `:app:testDebugUnitTest`, `:app:compileDebugKotlin`,
   `:app:compileDebugAndroidTestKotlin`.
3. Run it on the emulator (`./devtools app run`) and check the screen.
4. Present a summary and **wait for an explicit go-ahead before committing**.
5. On go-ahead: commit, push to `master`, bump `versionCode` by one and note it
   in the message. No changelog entry for behavior-preserving refactors (matches
   the repo's own precedent).

### Emulator notes

- `./devtools app run` builds, installs and launches; assume `emulator-5554` is
  up (start it with `./devtools emulator start` if needed).
- Reach a signed-in screen by seeding the session into the app DB:
  `adb shell run-as org.btcmap.debug sqlite3 databases/btcmap.db` with keys
  `auth_token` and `user` in table `pref`, then relaunch. Test account:
  `test-diver` / `qwertyui`.
- Verify visually via `adb shell uiautomator dump` + pulling the XML; sample
  pixels with PIL for colour checks.

## Next

Phases 1 and 2 are complete. The only remaining track is **Phase 3** (see its
plan above): migrate the map to MapLibre Compose and add the desktop target.
Nothing starts until Phase 3 is prioritised; it is gated on MapLibre Compose
maturing (desktop is Alpha).

Interim state: the `:ui:run` entry point renders only `StatsScreen`, so the
shared UI can be iterated on desktop already, but there is no desktop navigation
or map yet. `AuthErrorDialogFragment` and the embedded `MapView`s stay Views.
