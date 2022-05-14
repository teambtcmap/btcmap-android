import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.squareup.sqldelight")
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

dependencies {
    // Kotlin extensions
    // Simplifies non-blocking programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1")

    // Android extensions
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    val navVer = "2.4.2"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVer")
    implementation("androidx.navigation:navigation-ui-ktx:$navVer")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")

    // Modern HTTP client
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.9.3"))
    implementation("com.squareup.okhttp3:okhttp")

    // SQLDelight generates typesafe kotlin APIs from SQL statements
    val sqlDelightVer = "1.5.3"
    implementation("com.squareup.sqldelight:coroutines-extensions:$sqlDelightVer")
    implementation("com.squareup.sqldelight:android-driver:$sqlDelightVer")

    implementation("io.insert-koin:koin-android:3.2.0-beta-1")
    implementation("com.google.android.material:material:1.7.0-alpha01")
    implementation("org.osmdroid:osmdroid-android:6.1.11")
    implementation("com.google.code.gson:gson:2.9.0")
}