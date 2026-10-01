plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
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
            implementation(libs.gson)
            implementation(libs.okhttp.coroutines)
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.androidx.sqlite.bundled.jvm)
        }
    }
}
