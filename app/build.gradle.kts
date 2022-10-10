import java.io.FileInputStream
import java.net.URL
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("androidx.navigation.safeargs.kotlin")
    id("com.squareup.sqldelight")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

val signingPropertiesFile = rootProject.file("signing.properties")

android {
    namespace = "org.btcmap"
    compileSdk = 33

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 27
        targetSdk = 33
        versionCode = 17
        versionName = "0.4.2"
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
        if (signingPropertiesFile.exists()) {
            create("release") {
                val signingProperties = Properties()
                signingProperties.load(FileInputStream(signingPropertiesFile))
                storeFile = File(signingProperties["keystore_path"] as String)
                storePassword = signingProperties["keystore_password"] as String
                keyAlias = signingProperties["keystore_key_alias"] as String
                keyPassword = signingProperties["keystore_key_password"] as String
            }
        }

        create("selfSignedRelease") {
            storeFile = File(rootDir, "release.jks")
            storePassword = "btcmap"
            keyAlias = "btcmap"
            keyPassword = "btcmap"
        }
    }

    packagingOptions {
        resources.excludes += "DebugProbesKt.bin"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        if (signingPropertiesFile.exists()) {
            release {
                val signingProperties = Properties()
                signingProperties.load(FileInputStream(signingPropertiesFile))
                signingConfig = signingConfigs.getByName("release")
            }
        }

        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking
            isShrinkResources = true

            // Includes the default ProGuard rules file
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        create("selfSignedRelease") {
            signingConfig = signingConfigs.getByName("selfSignedRelease")

            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking
            isShrinkResources = true

            // Includes the default ProGuard rules file
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        create("googlePlayRelease") {
            applicationIdSuffix = ".app"

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

sqldelight {
    database("Database") {
        sourceFolders = listOf("sqldelight")
        packageName = "db"
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
    implementation("com.google.android.material:material:1.6.1")

    // Helps to split the app into multiple independent screens
    val fragmentVer = "1.5.2"
    implementation("androidx.fragment:fragment-ktx:$fragmentVer")
    debugImplementation("androidx.fragment:fragment-testing:$fragmentVer")

    // Modern HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // SQLDelight generates typesafe kotlin APIs from SQL statements
    val sqlDelightVer = "1.5.3"
    implementation("com.squareup.sqldelight:coroutines-extensions:$sqlDelightVer")
    implementation("com.squareup.sqldelight:android-driver:$sqlDelightVer")
    testImplementation("com.squareup.sqldelight:sqlite-driver:$sqlDelightVer")

    // Injection library
    implementation("io.insert-koin:koin-android:3.2.0")
    val koinAnnotationsVer = "1.0.1"
    implementation("io.insert-koin:koin-annotations:$koinAnnotationsVer")
    ksp("io.insert-koin:koin-ksp-compiler:$koinAnnotationsVer")

    // Open Street Map widget
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("com.github.MKergall:osmbonuspack:6.7.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Used to download, cache and display images
    implementation("com.squareup.picasso:picasso:2.8")

    // Common test dependencies
    testImplementation("junit:junit:4.13.2")

    // Common instrumented test dependencies
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.4.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}