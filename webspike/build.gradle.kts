import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Throwaway spike: proves the KMP-clean replacement stack needed to de-JVM
// `:shared` actually compiles and runs on a web target. Not part of the app.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    // The wasmJs target is what de-risks the port: it compiles the replacement
    // stack against the web backend. No nodejs()/browser() environment is
    // declared, because running wasm tests needs a Node download that this
    // project's repository policy blocks; the shared logic is covered on the
    // JVM instead.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
    }

    sourceSets {
        commonMain.dependencies {
            // OkHttp replacement.
            implementation(libs.ktor.client.core)
            // Gson replacement.
            implementation(libs.kotlinx.serialization.json)
            // java.time replacement.
            implementation(libs.kotlinx.datetime)
            // java.io replacement.
            implementation(libs.okio)
            // The DB interfaces `:shared` already uses.
            implementation(libs.androidx.sqlite)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.androidx.sqlite.bundled.jvm)
        }

        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.androidx.sqlite.web)
            }
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
