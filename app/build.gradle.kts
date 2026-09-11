import java.util.Properties

val keystoreProperties = Properties().apply {
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        localProperties.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.btcmap"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 29
        targetSdk = 37
        versionCode = 56
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release signing is optional: without a keystore in local.properties,
        // release and beta builds are produced unsigned, as per default and
        // expected behaviour in Android apps.
        val keystorePath = keystoreProperties.getProperty("release.keystore.path")
        if (keystorePath != null) {
            create("release") {
                storeFile = rootProject.file(keystorePath)
                storePassword = keystoreProperties.getProperty("release.keystore.password")
                keyAlias = keystoreProperties.getProperty("release.key.alias")
                keyPassword = keystoreProperties.getProperty("release.key.password")
            }
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
            signingConfig = signingConfigs.findByName("release")
        }

        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            manifestPlaceholders["appIcon"] = "@drawable/launcher_debug"
            manifestPlaceholders["appName"] = "@string/app_name_beta"
            signingConfig = signingConfigs.findByName("release")
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
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.framework)
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.mockwebserver)

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