import java.net.URI

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "org.btcmap"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 29
        targetSdk = 36
        versionCode = 54
        versionName = "0.9.2"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@drawable/launcher_debug"
        }

        release {
            manifestPlaceholders["appIcon"] = "@drawable/launcher"

            // https://developer.android.com/topic/performance/app-optimization/enable-app-optimization

            // Enables code-related app optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            // Includes the default ProGuard rules file
            proguardFiles(
                // Default file with automatically generated optimization rules.
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    dependencies {
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization)

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
        File(
            File(projectDir, "src/main/assets"),
            "bundled-places.json"
        ).writeText(
            URI("https://api.btcmap.org/v4/places?fields=id,lat,lon,icon,name,comments,boosted_until").toURL()
                .readText()
        )
    }
}