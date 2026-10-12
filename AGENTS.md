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

## Assumed Dev Environment
- Arch Linux (although other distros should work with no or minimal changes)
- A desktop session with X11/XWayland and working OpenGL (latest GNOME works, probably others)
- A JVM to bootstrap the Gradle wrapper (pacman --sync jdk-openjdk)

## Main Dev Interface (devtools script)

```bash
./devtools app run           # Run the app (desktop by default, under Compose Hot Reload; --android for the emulator, --cold for a plain desktop run)
./devtools app deploy-beta     # Build and rsync beta packages for the named platforms
./devtools app prepare-release # Build the release binaries for every supported platform into release/
./devtools app bundle-data       # Download latest places, areas, comments and events snapshots
./devtools app bundle-map-styles # Bundle MapLibre styles, sprites and glyphs
./devtools app bundle-icons      # Extract the icon-name list from the bundled font

./devtools emulator start    # Boot the AVD asynchronously

./devtools website deploy      # Build and rsync the documentation site to android.btcmap.org
```

### Compose Hot Reload (desktop)

The desktop run goes through **`:desktopApp:hotRun`** rather than `:desktopApp:run`, because Compose
Multiplatform 1.12.1 bundles Compose Hot Reload and applies it to any project with a desktop target, so
the running window can pick up an edit without a restart; `devtools` passes `--auto`, which keeps the
Gradle build up and reloads whenever a source file changes. `--no-auto` (forwarded to Gradle) leaves the
reload to `./gradlew :desktopApp:reload`, and `--cold` starts the plain run instead — for when the
javaagent or the dev-tools window would be in the way.

- **It needs the JetBrains Runtime, and this app needs JBR 25 specifically.** Only the JBR can redefine
  classes it has already loaded, and the JBR 21 that Compose Hot Reload would otherwise pick cannot load
  this project's Java 25 bytecode (`:desktopApp`, `:ui`'s JVM target and MapLibre Compose 0.20.0 are all
  class-file version 69). The toolchain resolver provisions the matching JBR into `~/.gradle/jdks` — a
  560 MB download, once — on the first hot run.
- **`--auto` is a continuous Gradle build.** It survives the shell that started it (both are backgrounded)
  and is stopped with the app: `pkill -f 'org.btcmap.desktop.MainK[t]'`.
- **`:ui` and `:shared` reload too**, not just `:desktopApp`: the plugin copies both jars into
  `desktopApp/build/run/main/classpath/libs`, so an edit to a screen or the map lands without a restart.
- **The hot task does not inherit `compose.desktop.application.jvmArgs`**, so the one that matters is set
  on the hot tasks directly in `:desktopApp` (`tasks.withType<ComposeHotRun> { jvmArgs(...) }`):
  `--enable-native-access=ALL-UNNAMED`, without which SQLite's bundled driver, MapLibre's FFI and Skiko
  warn that they are unrestricted. Anything else added to the application's `jvmArgs` from now on has to
  be repeated there. `hotRun` runs the app attached, so its output — and each reload's — goes to the
  Gradle output, `/tmp/btcmap-desktop.log` through `devtools`; only the async `hotRunAsync` detaches it
  into `desktopApp/build/run/main/hotRun.{stdout,stderr}.txt`.
- **The reload log is `desktopApp/build/run/main/main.chr.log`**, and it is where a reload that misbehaves
  shows its cause: each reload's Gradle output and the `@Composable` scopes it re-resolved land there.
- **An agent can drive the running app**: `./gradlew :desktopApp:hotMcpServer` serves the Compose Hot
  Reload MCP server over stdio (`reload`, `get_semantic_tree`, `click`, `type_text`, `get_logs`,
  `restart`, `reset_ui`, …), and it connects to whatever instance is already running. Its screenshot tool
  is not usable on a Wayland session — the JDK sends `java.awt.Robot` through the xdg-desktop-portal,
  which puts up a screen-sharing dialog and captures nothing useful here — so take screenshots by hand.

## Web Target (wasmJs)

Deferred until Compose 1.13 (MapLibre Compose has no wasmJs artifact, so `:ui`
cannot target the web)

### Cross-platform packaging (Conveyor)

The Windows and macOS packages are built by **Conveyor** (`desktopApp/conveyor.conf`, `dev.hydraulic.conveyor` in `desktopApp`), not by jpackage — jpackage cannot cross-compile, and silently *skips* the formats of a foreign OS, so on Linux only `.deb` (unused) and the AppImage exist. Conveyor produces a Windows MSIX + installer `.exe` + portable `.zip` and an Apple-Silicon `.app` archive with a Sparkle update feed, from any host OS. It is free for OSI-approved licences, which AGPL-3.0 is, and needs the EULA accepted (`--agree-to-license=1`, which `devtools` passes).

- **Versions and inputs come from Gradle.** `writeConveyorConfig` generates `desktopApp/generated.conveyor.conf` (gitignored — it holds absolute paths into this machine's Gradle cache), which `conveyor.conf` includes; `devtools` regenerates it on every deploy or prepare. It carries the app version from `app/build.gradle.kts`, the main class, `--enable-native-access=ALL-UNNAMED` and the per-platform inputs. `app.revision` is appended by the build script from the same `versionCode`, because MSIX refuses to install a package whose version fails to increase and a beta is rebuilt many times at one `versionName`.
- **The platform native runtimes are Conveyor configurations.** Skiko's and MapLibre's natives must be in `linuxAmd64`/`windowsAmd64`/`macAarch64` in `:desktopApp`, never on `implementation` or in `:ui`'s `jvmMain`: Conveyor reads the whole runtime classpath as the inputs every package shares, so a native jar declared in a library or on the shared classpath ends up inside the packages for the other platforms too. The plugin adds whichever configuration matches the build machine back to `implementation`, so `run`, the tests and the AppImage still work. `:ui`'s `jvmTest` carries this host's natives because the library itself deliberately carries none.
- **Only Windows x64 and Apple-Silicon macOS are supported.** MapLibre Compose publishes no macOS x64 native runtime (nor does `androidx.sqlite`'s bundled JVM driver), so Intel Macs cannot run the app at all; `app.machines` is restricted accordingly. Linux is still packaged by jpackage as the AppImage.
- **The signing root key is per project, kept out of the repository.** `local.properties` points at it with `conveyor.signing.key` — beside the Android keystore, never in the checkout — and `:desktopApp`'s `writeConveyorConfig` appends an `include` of it to the generated config, so the `devtools` builds and a manual `conveyor -f desktopApp/conveyor.conf` build both sign with it. It must be backed up: every Windows and macOS package key, the update feeds and the Windows package identity derive from it, so losing or rotating it means already-installed copies can never be updated and Windows treats the app as a different one. `devtools` generates it at that path on first use if the file is missing. A key protected with a passphrase (`conveyor keys passphrase`) needs `CONVEYOR_PASSPHRASE` exported.
- **The packaged Windows build does not start, and that is left to an upstream fix.** Conveyor bakes `-Djava.library.path=<app dir>;<root>\bin` into a packaged launcher, so the system directories a plain JVM has implicitly are gone. MapLibre's Windows runtime then fails to load what it needs — `maplibre-native-c.dll` imports USER32, GDI32, OPENGL32, CRYPT32 and the COM/WinRT layer behind them, and its loader also loads host graphics libraries (`vulkan-1.dll`, `libEGL.dll`, …) by name — and because nothing escapes `main`, the packaged app simply runs with no window rather than reporting anything. Both local workarounds were tried and deliberately reverted instead of being guessed at and shipped: appending the system directories at runtime, from `SystemRoot`, to `java.library.path` and to MapLibre's `org.maplibre.nativeffi.library.dirs`, and setting `java.library.path` through Conveyor's config (which means naming `C:\Windows` literally — Conveyor resolves `${env.*}` while reading the config on the build machine, and its launcher expands no environment variables — and pins the launcher's own directory list). Do not reintroduce either without a Windows machine to verify on; wait for MapLibre Compose or Conveyor to fix it. Until then Windows can still be investigated with `CONVEYOR_DEBUG=1 ./devtools app deploy-beta --windows`, which builds the same packages with `-XX:-DisableAttachMechanism`, a console and `jcmd` enabled (`conveyor-debug.conf`).
- **Uploads never delete from the site.** `conveyor make site` emits only the current version's files and keeps the older ones on the server, because it downloads them to build delta updates; the site directory is also where the channel's base URL points, since Conveyor bakes that URL into the packages and an installed copy only ever checks its own channel.

## Bundled Assets

Places, areas, comments, events and map styles are
committed as assets and refreshed manually with `./devtools app bundle-data` and
`./devtools app bundle-map-styles`, and the icon-name list with
`./devtools app bundle-icons`. This is
intentional:

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

## Testing
- Tests are fast, run and write liberally, but never run instrumented tests (`connectedDebugAndroidTest`) unless the user
  explicitly asks for them since those are slow.
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

## Terminal

- never use `sleep` for more than a second, it creates unresanoble delays