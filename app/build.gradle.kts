import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.btcmap"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 27
        targetSdk = 34
        versionCode = 49
        versionName = "0.7.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
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
        resources.excludes += "DebugProbesKt.bin"

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

        val elementsSrc = URL("https://static.btcmap.org/api/v3/elements.json")
        File(destDir, "elements.json").writeText(elementsSrc.readText())

        val reportsSrc = URL("https://static.btcmap.org/api/v2/reports.json")
        File(destDir, "reports.json").writeText(reportsSrc.readText())

        val eventsSrc = URL("https://static.btcmap.org/api/v2/events.json")
        File(destDir, "events.json").writeText(eventsSrc.readText())

        val areasSrc = URL("https://static.btcmap.org/api/v2/areas.json")
        File(destDir, "areas.json").writeText(areasSrc.readText())
    }
}

dependencies {
    // Allows suspending functions
    // https://github.com/Kotlin/kotlinx.coroutines/releases
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Simplifies in-app navigation
    // https://developer.android.com/jetpack/androidx/releases/navigation
    val navVer = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVer")
    implementation("androidx.navigation:navigation-ui-ktx:$navVer")

    // Helps with keeping our view hierarchies flat
    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Used by osmdroid (original prefs API is deprecated)
    // https://developer.android.com/jetpack/androidx/releases/preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Material design components
    // https://github.com/material-components/material-components-android/releases
    implementation("com.google.android.material:material:1.11.0")

    // Helps to split the app into multiple independent screens
    // https://developer.android.com/jetpack/androidx/releases/fragment
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")

    // Modern HTTP client
    // https://github.com/square/okhttp/blob/master/CHANGELOG.md
    val okhttpVer = "5.0.0-alpha.14"
    implementation("com.squareup.okhttp3:okhttp-coroutines:$okhttpVer")
    implementation("com.squareup.okhttp3:okhttp-brotli:$okhttpVer")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVer")

    // Injection library
    // https://github.com/InsertKoinIO/koin/blob/main/CHANGELOG.md
    implementation("io.insert-koin:koin-android:3.5.0")

    // Open Street Map widget
    // https://github.com/osmdroid/osmdroid/releases
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Map utilities
    // https://github.com/locationtech/jts/releases
    implementation("org.locationtech.jts:jts-core:1.19.0")

    // Charts
    // https://github.com/PhilJay/MPAndroidChart/releases
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Used to cache data and store user preferences
    // https://developer.android.com/kotlin/ktx#sqlite
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Bundle SQLite binaries
    // https://github.com/requery/sqlite-android/releases
    // TODO remove bundled SQLite when Android bumps its deps
    // > The JSON functions and operators are built into SQLite by default, as of SQLite version 3.38.0 (2022-02-22).
    // API 33 -> 3.32
    // API 34 -> 3.39 (~45% of our install base)
    // https://www.sqlite.org/json1.html
    implementation("com.github.requery:sqlite-android:3.45.0")

    // Used to download, cache and display images
    // https://github.com/coil-kt/coil/releases
    val coilVer = "2.6.0"
    implementation("io.coil-kt:coil:$coilVer")
    implementation("io.coil-kt:coil-svg:$coilVer")

    // Background job scheduler
    // Used to fetch new data in background
    // https://developer.android.com/jetpack/androidx/releases/work
    val workVer = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$workVer")

    // Common test dependencies
    // https://junit.org/junit4/
    val junitVer = "4.13.2"
    testImplementation("junit:junit:$junitVer")
    testImplementation("org.json:json:20231013")
}