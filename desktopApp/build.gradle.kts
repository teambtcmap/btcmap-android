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

// The run task launches the project's toolchain, so it needs the Java 25 one
// rather than the Gradle daemon's.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    // The SQLite driver the shared database runs on off Android.
    implementation(libs.androidx.sqlite.bundled.jvm)
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
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BTC Map"
        }
    }
}
