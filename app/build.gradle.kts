import java.net.URI

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.btcmap"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 29
        targetSdk = 36
        versionCode = 56
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@drawable/launcher_debug"
        }

        release {
            manifestPlaceholders["appIcon"] = "@drawable/launcher"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.framework)
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    implementation(libs.androidx.fragment)
    testImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)

    implementation(libs.material)
    implementation(libs.okhttp.coroutines)
    implementation(libs.okhttp.brotli)
    testImplementation(libs.mockwebserver)
    implementation(libs.maplibre)
    implementation(libs.qrgenerator)
    implementation(libs.colorpicker)
    implementation(libs.coil)
    implementation(libs.coil.network)
    implementation(libs.coil.svg)
    implementation(libs.gson)
    testImplementation(libs.junit)
}

tasks.register<DefaultTask>("bundleData") {
    outputs.file(File(projectDir, "src/main/assets/bundled-places.json"))
    doLast {
        val dir = File(projectDir, "src/main/assets")
        dir.mkdirs()
        File(dir, "bundled-places.json").writeText(
            URI("https://api.btcmap.org/v4/places?fields=id,lat,lon,icon,name,comments,boosted_until").toURL()
                .readText()
        )
    }
}