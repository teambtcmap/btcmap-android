# Migration Plan: Compose (Multiplatform) and a Shared Module

Status: Phase 1 in progress. The `:shared` module exists and its first slice has
landed; `:app` builds, the moved tests pass, and the app runs on the emulator
(see Progress).

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
| `androidx.sqlite` + `AndroidSQLiteDriver` | Driver is platform-specific | `expect`/`actual`; use `BundledSQLiteDriver` on JVM |

The only real work is removing `android.*` / `androidx.*`: `Context` /
`AssetManager` (bundle seeding), `AndroidSQLiteDriver`, and `Fragment`.

This was verified empirically (see Progress): with only Android and JVM
targets, `commonMain` **accepts `java.*`** (the common metadata compilation is
skipped) but **rejects `android.*`**. So the JVM-bound logic — `java.time`,
Gson, OkHttp, `java.io` — moves to `commonMain` unchanged; only the Android
references have to be cut.

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

- Two Gradle modules: `:app` (the Android app — all UI and platform glue) and
  `:shared` (`commonMain`, with Android and JVM targets). ~193 Kotlin files,
  ~21k lines total.
- So far only `search/NameMatch` has been extracted (see Progress); the rest of
  the layout below is still as it was before the migration started.
- UI: Android Views + ViewBinding + Fragments, `RecyclerView`, Material
  Components. No Compose.
- ~100 files have no `android.*` / `androidx.*` import; ~90 are Android-coupled
  UI.
- Large Android-MapLibre-specific map stack: `MapFragment` (947 lines),
  `MarkerIcon` (420), `OfflineMaps` (328), `map/layer/*`, `MapSetupController`,
  bundled PMTiles basemap.
- 96 unit-test files; tests are the default verification.

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

- SQLite driver (`AndroidSQLiteDriver` vs `BundledSQLiteDriver`) and DB path.
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

### Phase 3 — Map to MapLibre Compose

`maplibre/maplibre-compose` (the official MapLibre org wrapper for Compose
Multiplatform) now targets Android, iOS, and Desktop (JVM) on one shared
implementation and API. Desktop is **Alpha** and the API is explicitly unstable;
desktop requires Java 25 and `--enable-native-access=ALL-UNNAMED`.

In scope for the rewrite:

- `map/layer/*` (merchant, event, exchange layers)
- `MarkerIcon`, `MarkerGeoJson`, `EventGeoJson`, viewport/geometry caches
- `MapSetupController`, selection/search/bottom-sheet controllers
- `OfflineMaps` and the bundled PMTiles basemap (no `AssetManager` on desktop —
  needs a different cache strategy)

Expected effort: **1–3 months** focused, solo. Very low confidence.

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

## Open decisions

These change the estimates and should be settled before starting Phase 2:

1. Hours per week, and whether this is solo.
2. Must the desktop app have an interactive map, or can it be a browse/search/
   data client without one? (Removes Phase 3.)
3. Does the Android UI ultimately move to Compose Multiplatform, or stay on
   Views while desktop gets its own CMP UI? (Affects Phase 2 cost and UI
   duplication.)

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

Not yet done: `openinghours` can actually move too (it only needs `java.time`,
which `commonMain` accepts here); it was not part of this first slice.

## Suggested next step

Continue Phase 1 with a larger, cohesive slice: move `openinghours` (all four
files plus `OpeningHoursTest`) to `commonMain`, then `stats` and
`db/table/*`. Each is verified with `:shared:jvmTest` and an `:app` test run.
