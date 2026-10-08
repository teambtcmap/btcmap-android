import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // Applied without a version: the Kotlin plugins are already on the build's
    // classpath through the multiplatform modules.
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    // MapLibre Compose's desktop artifact is Java 25 bytecode.
    jvmToolchain(25)
}

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
    implementation(compose.desktop.currentOs)
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
    runtimeOnly(libs.maplibre.compose.runtime.opengl.linux.x64)
    // The SQLite driver the shared database runs on off Android.
    implementation(libs.androidx.sqlite.bundled.jvm)
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
        // Where the icon font (the Android asset) lives while running from the
        // sources; packaging it into the distribution is still to do.
        jvmArgs += "-Dbtcmap.iconFontDir=" + rootProject.file("app/src/main/assets").absolutePath
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BTC Map"
        }
    }
}
