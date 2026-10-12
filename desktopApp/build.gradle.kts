import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    // Applied without a version: the Kotlin plugins are already on the build's
    // classpath through the multiplatform modules.
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.jetbrains.compose)
    // Cross-builds the Windows and macOS packages, with their update feeds, from
    // any host OS. The Compose plugin's own jpackage tasks cannot do that: it
    // has no cross-compilation and silently skips the foreign-OS formats.
    alias(libs.plugins.conveyor)
}

kotlin {
    // MapLibre Compose's desktop artifact is Java 25 bytecode.
    jvmToolchain(25)
}

// Conveyor requires a version (every package format needs one) and takes it from
// the Gradle project. Read it from the Android app instead of repeating it here,
// so the desktop packages can never drift from the APKs; the versionCode becomes
// the Conveyor revision below, because MSIX refuses to install a package whose
// version fails to increase and the versionCode already rises with every app
// change.
private val appBuildScript = rootProject.file("app/build.gradle.kts").readText()
private val appVersionName = Regex("""versionName\s*=\s*"([^"]+)"""")
    .find(appBuildScript)?.groupValues?.get(1)
    ?: error("could not read versionName from app/build.gradle.kts")
private val appVersionCode = Regex("""versionCode\s*=\s*(\d+)""")
    .find(appBuildScript)?.groupValues?.get(1)?.toInt()
    ?: error("could not read versionCode from app/build.gradle.kts")

version = appVersionName

// The Conveyor signing root key, from which every Windows and macOS package key,
// update feed and package identity is derived. It is signing material, so it
// lives outside the repository (beside the Android keystore) and is pointed at
// from local.properties; the generated config includes it. Without this Conveyor
// would fall back to its own per-user directory and mint a second, different
// identity, which would look like a different app to Windows.
private val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
private val conveyorSigningKey = localProperties.getProperty("conveyor.signing.key")
    ?.let { rootProject.file(it) }

// The Android assets the desktop app reuses: the icon font, the map styles and
// the bundled data snapshots. They are copied in as resources so the packaged
// app carries them instead of reading them from the sources.
val bundleAndroidAssets by tasks.registering(Copy::class) {
    from(rootProject.file("app/src/main/assets")) {
        include("material-symbols-*.ttf")
        rename { "material-symbols.ttf" }
    }
    // The snapshots let the desktop seed its database offline, as Android does,
    // instead of pulling the whole delta history on first run.
    from(rootProject.file("app/src/main/assets")) {
        include("bundled-*.json")
    }
    from(rootProject.file("app/src/main/assets/map-styles")) {
        into("map-styles")
    }
    into(layout.buildDirectory.dir("generated/android-assets"))
}

sourceSets.main {
    resources.srcDir(bundleAndroidAssets)
}

// The run task launches the project's toolchain, so it needs the Java 25 one
// rather than the Gradle daemon's.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

@OptIn(ExperimentalComposeLibrary::class)
dependencies {
    implementation(project(":shared"))
    implementation(project(":ui"))
    implementation(compose.material3)
    // Coil has no built-in network fetcher on the JVM, so the remote images the
    // shared screens request (area chips, place photos, search results) never
    // load without this on the classpath. Coil's ServiceLoader picks it up, as
    // it does for `:app` on Android.
    implementation(libs.coil.network)
    // Country flags and some area icons are SVG and the API returns them as-is,
    // so the shared screens need the SVG decoder to draw those chips.
    implementation(libs.coil.svg)
    implementation(libs.maplibre.compose)
    // Provides Dispatchers.Main on the AWT event thread, which the map's engine
    // callbacks need.
    implementation(libs.kotlinx.coroutines.swing)
    // The SQLite driver the shared database runs on off Android. Its single jar
    // carries the natives for every platform this app targets.
    implementation(libs.androidx.sqlite.bundled.jvm)

    // Skiko and MapLibre ship their native runtimes as per-platform artifacts.
    // They deliberately do not go on `implementation`: the Conveyor plugin reads
    // the runtime classpath as the list of inputs shared by every package, so a
    // Linux `.so` put there directly would be shipped inside the Windows and
    // macOS packages too. Each platform's runtimes go in the matching Conveyor
    // configuration instead, and the plugin adds whichever one matches the
    // machine running Gradle back to `implementation` — so `run`, the tests and
    // the Linux AppImage keep working unchanged.
    "linuxAmd64"(compose.desktop.linux_x64)
    "linuxAmd64"(libs.maplibre.compose.runtime.opengl.linux.x64)
    "windowsAmd64"(compose.desktop.windows_x64)
    "windowsAmd64"(libs.maplibre.compose.runtime.opengl.windows.x64)
    "macAarch64"(compose.desktop.macos_arm64)
    "macAarch64"(libs.maplibre.compose.runtime.opengl.macos.arm64)

    // Compose desktop UI tests: `runComposeUiTest` drives the screens without a
    // window, so the desktop wiring can be clicked through on the JVM.
    testImplementation(compose.uiTest)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    // The remote-image test builds Coil's loader directly; the main source set
    // only sees Coil transitively through `:ui`.
    testImplementation(libs.coil)
    testImplementation(libs.mockwebserver)
}

tasks.test {
    // Skiko, which the Compose test scene renders through, uses the FFM API.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Conveyor takes the package version from the Gradle project version, and the
// revision from nothing at all. Append the Android versionCode as the revision
// to the config the plugin generates: a beta is rebuilt many times at the same
// versionName, and MSIX refuses to install a package whose version fails to
// increase, so without this a rebuilt beta would never reach anyone who already
// had the previous one. The signing root key is included the same way, so a
// manual `conveyor -f desktopApp/conveyor.conf` build signs with it too.
tasks.named<hydraulic.conveyor.gradle.WriteConveyorConfigTask>("writeConveyorConfig") {
    // Everything the generated config bakes in has to be an input, or a change
    // (a moved signing key, a bumped versionCode) would leave the task UP-TO-DATE
    // and the old value in the file — which for the revision means Conveyor
    // rebuilding a version it has already uploaded, and refusing to.
    inputs.property("conveyorSigningKey", conveyorSigningKey?.absolutePath ?: "")
    inputs.property("appVersionCode", appVersionCode)
    doLast {
        val config = destination.get().asFile
        config.appendText("\n// Appended by :desktopApp/build.gradle.kts.\napp.revision = $appVersionCode\n")

        conveyorSigningKey?.let { key ->
            check(key.isFile) {
                "conveyor.signing.key points at $key, which does not exist. Generate the Conveyor " +
                    "signing root key (once) with:\n" +
                    "  conveyor --agree-to-license=1 keys generate --no-passphrase -o ${key.parentFile}"
            }
            val path = key.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
            config.appendText("include required(\"$path\")\n")
        }
    }
}

// Renders one screen to a PNG without a window, so the desktop UI can be checked
// from a headless run: `./gradlew :desktopApp:screenshot -Pscreenshot=<screen>:<path>`.
tasks.register<JavaExec>("screenshot") {
    group = "verification"
    description = "Renders a desktop screen to a PNG."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.btcmap.desktop.MainKt")
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        },
    )
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // The render opens the app database. Never point it at the live
    // `$BTCMAP_HOME`/`~/.btcmap` while the app is running: a second process
    // opening the same file could once make Database.initialize mistake a locked
    // database for a foreign one and delete it. Give the render its own home so
    // it cannot touch the user's data; override with -PbtcmapHome=<dir>.
    environment(
        "BTCMAP_HOME",
        project.findProperty("btcmapHome")?.toString()
            ?: project.layout.buildDirectory.dir("screenshot-home").get().asFile.absolutePath,
    )
    args = listOf(
        "--screenshot=" + (
            project.findProperty("screenshot")?.toString()
                ?: "settings:${layout.buildDirectory.get().asFile}/desktop-screen.png"
            ),
    )
}

compose.desktop {
    application {
        mainClass = "org.btcmap.desktop.MainKt"
        // The app is Java 25 bytecode, so `run` must use the Java 25 toolchain
        // rather than the Gradle daemon's JVM.
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        }.get().metadata.installationPath.asFile.absolutePath
        // MapLibre Native reaches its native libraries through the FFI.
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        // Where the icon font (the Android asset) lives when running from the
        // sources. The distribution carries it as a resource instead, which
        // loadIconFont() prefers, so this only matters for `run`.
        jvmArgs += "-Dbtcmap.iconFontDir=" + rootProject.file("app/src/main/assets").absolutePath
        nativeDistributions {
            // Linux only: jpackage cannot cross-compile, so Dmg and Msi were
            // silently skipped on this machine. The Windows and macOS packages
            // come from Conveyor instead (see conveyor.conf).
            targetFormats(TargetFormat.Deb)
            packageName = "BTC Map"
        }
    }
}
