import java.io.FileInputStream
import java.net.URL
import java.util.Properties
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("com.android.application")
    kotlin("android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.squareup.sqldelight")
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
}

val signingPropertiesFile = rootProject.file("signing.properties")

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "0.1.0"
        setProperty("archivesBaseName", "btcmap-$versionName")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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
            proguardFile(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

sqldelight {
    database("Database") {
        sourceFolders = listOf("sqldelight")
        packageName = "db"
        deriveSchemaFromMigrations = true
    }
}

tasks.register("refreshData") {
    doLast {
        val assetsDir = File("app/src/main/assets")
        if (!assetsDir.exists()) assetsDir.mkdir()
        val url = URL("https://raw.githubusercontent.com/bubelov/btcmap-data/main/data.json")
        url.openStream().use { Files.copy(it, Paths.get("app/src/main/assets/data.json")) }
    }
}

dependencies {
    // Kotlin extensions
    // Simplifies non-blocking programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1")

    // Android extensions
    implementation("androidx.core:core-ktx:1.7.0")
    val navVer = "2.4.2"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVer")
    implementation("androidx.navigation:navigation-ui-ktx:$navVer")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.android.material:material:1.6.0")

    // Modern HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // SQLDelight generates typesafe kotlin APIs from SQL statements
    val sqlDelightVer = "1.5.3"
    implementation("com.squareup.sqldelight:coroutines-extensions:$sqlDelightVer")
    implementation("com.squareup.sqldelight:android-driver:$sqlDelightVer")

    // Injection library
    implementation("io.insert-koin:koin-android:3.2.0")
    val koinAnnotationsVer = "1.0.0-beta-2"
    implementation("io.insert-koin:koin-annotations:$koinAnnotationsVer")
    ksp("io.insert-koin:koin-ksp-compiler:$koinAnnotationsVer")

    // Open Street Map widget
    implementation("org.osmdroid:osmdroid-android:6.1.13")
}