import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"
    id("androidx.navigation.safeargs.kotlin")
    id("app.cash.sqldelight")
    id("com.google.devtools.ksp") version "1.7.20-1.0.7"
}

android {
    namespace = "org.btcmap"
    compileSdk = 33

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 27
        targetSdk = 33
        versionCode = 34
        versionName = "0.5.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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

    packagingOptions {
        resources.excludes += "DebugProbesKt.bin"

        jniLibs.excludes += "/lib/armeabi-v7a/libsqlite3x.so"
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
        release {
            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking
            isShrinkResources = true

            // Includes the default ProGuard rules file
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    sourceSets.all {
        kotlin.srcDir("build/generated/ksp/$name/kotlin")
    }
}

val sqlDelightVer = "2.0.0-alpha04"

sqldelight {
    database("Database") {
        sourceFolders = listOf("sqldelight")
        packageName = "db"
        dialect("app.cash.sqldelight:sqlite-3-35-dialect:$sqlDelightVer")
        module("app.cash.sqldelight:sqlite-json-module:$sqlDelightVer")
    }
}

tasks.register("bundleData") {
    doLast {
        val src = URL("https://api.btcmap.org/v2/elements")
        val destDir = File(projectDir, "src/main/assets")
        destDir.mkdirs()
        val destFile = File(destDir, "elements.json")
        destFile.writeText(src.readText())
    }
}

dependencies {
    // Simplifies non-blocking programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Platform-agnostic JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    // Kotlin extensions for Android SDK
    implementation("androidx.core:core-ktx:1.9.0")

    // Simplifies in-app navigation
    val navVer = "2.5.2"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVer")
    implementation("androidx.navigation:navigation-ui-ktx:$navVer")

    // Helps with keeping our view hierarchies flat
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Used by osmdroid
    implementation("androidx.preference:preference-ktx:1.2.0")

    // Material design components
    implementation("com.google.android.material:material:1.7.0")

    // Helps to split the app into multiple independent screens
    val fragmentVer = "1.5.2"
    implementation("androidx.fragment:fragment-ktx:$fragmentVer")
    debugImplementation("androidx.fragment:fragment-testing:$fragmentVer")

    // Modern HTTP client
    implementation("com.squareup.okhttp3:okhttp-brotli:4.10.0")

    // SQLDelight generates typesafe kotlin APIs from SQL statements
    implementation("app.cash.sqldelight:coroutines-extensions:$sqlDelightVer")
    implementation("app.cash.sqldelight:android-driver:$sqlDelightVer")
    testImplementation("app.cash.sqldelight:sqlite-driver:$sqlDelightVer")

    // Injection library
    implementation("io.insert-koin:koin-android:3.2.2")
    val koinAnnotationsVer = "1.0.1"
    implementation("io.insert-koin:koin-annotations:$koinAnnotationsVer")
    ksp("io.insert-koin:koin-ksp-compiler:$koinAnnotationsVer")

    // Open Street Map widget
    implementation("org.osmdroid:osmdroid-android:6.1.14")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Bundle SQLite binaries
    implementation("com.github.requery:sqlite-android:3.39.2")

    // Used to download, cache and display images
    implementation("io.coil-kt:coil:2.2.2")
    implementation("io.coil-kt:coil-svg:2.2.2")

    // Common test dependencies
    testImplementation("junit:junit:4.13.2")

    // Common instrumented test dependencies
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.4.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}