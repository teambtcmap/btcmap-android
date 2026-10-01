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

## Working notes

Durable facts and conventions for continuing the migration.

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
