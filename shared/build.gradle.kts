import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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

    // The web target the de-JVMing and async data-layer work was for.
    // Compile-only for now: the browser test environment downloads Node, which
    // the repository policy blocks, and the Compose UI (which owns the map) has
    // no Web target yet.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.sqlite)
            // Multiplatform JSON, the Gson replacement. Exposed because the
            // shared DTOs carry kotlinx.serialization `JsonObject` fields.
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.json.okio)
            // Multiplatform HTTP, the OkHttp replacement. Exposed because the
            // shared DTOs carry Ktor `Url` fields.
            api(libs.ktor.client.core)
            // Multiplatform date/time, the java.time replacement.
            implementation(libs.kotlinx.datetime)
            // The java.io replacement: streaming sources and file access.
            implementation(libs.okio)
            // The java.util.concurrent replacement: atomics and locks.
            implementation(libs.atomicfu)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }

        getByName("wasmJsMain") {
            dependencies {
                // The browser HTTP engine the shared client resolves at runtime.
                implementation(libs.ktor.client.js)
            }
        }

        jvmTest.dependencies {
            implementation(libs.androidx.sqlite.bundled.jvm)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockwebserver)
        }
    }
}
