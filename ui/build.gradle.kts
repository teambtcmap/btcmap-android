plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "org.btcmap.ui"
        compileSdk = 37
        minSdk = 29
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            // Exposed because the Android app hosts these composables through a
            // ComposeView, which needs the Compose types on its classpath.
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.btcmap.ui.MainKt"
    }
}
