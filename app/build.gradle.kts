import java.net.URI
import java.util.Properties

val keystoreProperties = Properties().apply {
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        load(localProperties.inputStream())
    }
}

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

    signingConfigs {
        create("release") {
            val keystorePath = keystoreProperties.getProperty("release.keystore.path")
                ?: error("release.keystore.path must be set in local.properties")
            storeFile = rootProject.file(keystorePath)
            storePassword = keystoreProperties.getProperty("release.keystore.password")
                ?: error("release.keystore.password must be set in local.properties")
            keyAlias = keystoreProperties.getProperty("release.key.alias")
                ?: error("release.key.alias must be set in local.properties")
            keyPassword = keystoreProperties.getProperty("release.key.password")
                ?: error("release.key.password must be set in local.properties")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@drawable/launcher_debug"
            manifestPlaceholders["appName"] = "@string/app_name"
        }

        release {
            manifestPlaceholders["appIcon"] = "@drawable/launcher"
            manifestPlaceholders["appName"] = "@string/app_name"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }

        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            manifestPlaceholders["appIcon"] = "@drawable/launcher_debug"
            manifestPlaceholders["appName"] = "@string/app_name_beta"
            signingConfig = signingConfigs.getByName("release")
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

androidComponents {
    onVariants { variant ->
        if (variant.name == "debug") {
            return@onVariants
        }

        for (output in variant.outputs) {
            if (output.outputFileName.get().contains("universal")) {
                output.outputFileName.set("btcmap-${output.versionName.get()}-universal.apk")
            }

            if (output.outputFileName.get().contains("arm64-v8a")) {
                output.outputFileName.set("btcmap-${output.versionName.get()}-arm.apk")
            }

            if (output.outputFileName.get().contains("x86_64")) {
                output.outputFileName.set("btcmap-${output.versionName.get()}-x86.apk")
            }
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
    implementation(libs.cronutils)
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