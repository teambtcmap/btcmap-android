import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "org.btcmap"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 27
        targetSdk = 34
        versionCode = 50
        versionName = "0.7.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }

    signingConfigs {
        create("selfSigned") {
            storeFile = File(rootDir, "release.jks")
            storePassword = "btcmap"
            keyAlias = "btcmap"
            keyPassword = "btcmap"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }

        // TODO remove bundled SQLite when Android bumps its deps
        // > The JSON functions and operators are built into SQLite by default, as of SQLite version 3.38.0 (2022-02-22).
        // https://www.sqlite.org/json1.html
        //jniLibs.excludes += "/lib/armeabi-v7a/libsqlite3x.so"
        //jniLibs.excludes += "lib/arm64-v8a/libsqlite3x.so"
        jniLibs.excludes += "/lib/x86/**"
        //jniLibs.excludes += "/lib/x86_64/**"
    }

    flavorDimensions += "store"
    flavorDimensions += "signature"

    productFlavors {
        create("fdroid") {
            dimension = "store"
        }

        create("play") {
            dimension = "store"
            applicationIdSuffix = ".app"
        }

        create("selfSigned") {
            dimension = "signature"
            signingConfig = signingConfigs.getByName("selfSigned")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking
            isShrinkResources = true

            // Includes the default ProGuard rules file
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

tasks.register("bundleData") {
    doLast {
        val destDir = File(projectDir, "src/main/assets")
        destDir.mkdirs()

        val elementsSrc = URI("https://static.btcmap.org/api/v3/elements.json")
        File(destDir, "elements.json").writeText(elementsSrc.toURL().readText())

        val reportsSrc = URI("https://static.btcmap.org/api/v2/reports.json")
        File(destDir, "reports.json").writeText(reportsSrc.toURL().readText())

        val eventsSrc = URI("https://static.btcmap.org/api/v2/events.json")
        File(destDir, "events.json").writeText(eventsSrc.toURL().readText())

        val areasSrc = URI("https://static.btcmap.org/api/v2/areas.json")
        File(destDir, "areas.json").writeText(areasSrc.toURL().readText())

        val usersSrc = URI("https://static.btcmap.org/api/v2/users.json")
        File(destDir, "users.json").writeText(usersSrc.toURL().readText())
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.material)
    implementation(libs.okhttp.coroutines)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.mockwebserver)
    implementation(libs.koin)
    implementation(libs.osmdroid)
    implementation(libs.jts)
    implementation(libs.mpandroidchart)
    implementation(libs.sqlite)
    implementation(libs.coil.core)
    implementation(libs.coil.svg)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}