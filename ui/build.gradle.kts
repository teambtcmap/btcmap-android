import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    // MapLibre Compose's desktop artifact is Java 25 bytecode, so the toolchain
    // must be at least 25; the Android target still emits JVM 17 bytecode.
    jvmToolchain(25)

    android {
        namespace = "org.btcmap.ui"
        compileSdk = 37
        minSdk = 29

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            // Exposed because the Android app hosts these composables through a
            // ComposeView, which needs the Compose types on its classpath.
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.maplibre.compose)
            implementation(libs.coil.compose)
            implementation(libs.okhttp)
        }

        androidMain.dependencies {
            runtimeOnly(libs.maplibre.compose.runtime.opengl.android)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            runtimeOnly(libs.maplibre.compose.runtime.opengl.linux.x64)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.btcmap.ui.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
    }
}
