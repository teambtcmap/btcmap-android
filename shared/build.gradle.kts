plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)

    // Android target for the Android app. The block moved from `androidTarget()`
    // to `android {}` with the Android-KMP library plugin (AGP 9).
    android {
        namespace = "org.btcmap.shared"
        compileSdk = 37
        minSdk = 29
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.sqlite)
            // Multiplatform JSON, the Gson replacement.
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.json.okio)
            // The java.io replacement: streaming sources and file access.
            implementation(libs.okio)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.okhttp.brotli)
            implementation(libs.okhttp.coroutines)
        }

        jvmTest.dependencies {
            implementation(libs.androidx.sqlite.bundled.jvm)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockwebserver)
        }
    }
}
