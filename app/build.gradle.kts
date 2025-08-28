import java.net.URI

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "org.btcmap"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 29
        targetSdk = 35
        versionCode = 54
        versionName = "0.9.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin.compilerOptions.optIn.add("kotlin.time.ExperimentalTime")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }

    flavorDimensions += "store"

    productFlavors {
        create("fdroid") {
            dimension = "store"
        }

        create("play") {
            dimension = "store"
            applicationIdSuffix = ".app"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@drawable/launcher_debug"
        }

        release {
            manifestPlaceholders["appIcon"] = "@drawable/launcher"

            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking
            isShrinkResources = true

            // Includes the default ProGuard rules file
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    dependenciesInfo {
        includeInApk = false
    }

    dependencies {
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization)
        implementation(libs.kotlinx.datetime)

        implementation(libs.androidx.work)
        implementation(libs.androidx.sqlite)

        implementation(libs.material)
        implementation(libs.okhttp.coroutines)
        implementation(libs.okhttp.brotli)
        implementation(libs.okhttp.mockwebserver)
        implementation(libs.koin)
        implementation(libs.mpandroidchart)
        implementation(libs.coil.core)
        implementation(libs.coil.svg)
        implementation(libs.maplibre)
        implementation(libs.qrgenerator)
        implementation(libs.colorpicker)
    }
}

tasks.register("bundleData") {
    doLast {
        val destDir = File(projectDir, "src/main/assets")
        destDir.mkdirs()
        val placesSrc =
            URI("https://api.btcmap.org/v4/places?fields=id,lat,lon,icon,name,comments,boosted_until")
        File(destDir, "bundled-places.json").writeText(placesSrc.toURL().readText())
    }
}