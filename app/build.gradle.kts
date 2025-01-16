import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
                freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines)

            implementation(libs.androidx.navigation.fragment)
            implementation(libs.androidx.navigation.ui)
            implementation(libs.androidx.work)
            implementation(libs.androidx.constraintlayout)
            implementation(libs.androidx.room)

            implementation(libs.material)
            implementation(libs.okhttp.coroutines)
            implementation(libs.okhttp.brotli)
            implementation(libs.okhttp.mockwebserver)
            implementation(libs.koin)
            implementation(libs.jts)
            implementation(libs.mpandroidchart)
            implementation(libs.coil.core)
            implementation(libs.coil.svg)
            implementation(libs.maplibre)
            implementation(libs.qrgenerator)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.datetime)

            implementation(libs.androidx.sqlite)
        }
    }
}

android {
    namespace = "org.btcmap"
    compileSdk = 35

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.btcmap"
        minSdk = 27
        targetSdk = 35
        versionCode = 52
        versionName = "0.9.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
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
        val destDir = File(projectDir, "src/androidMain/assets")
        destDir.mkdirs()

        val elementsSrc = URI("https://static.btcmap.org/api/v3/elements.json")
        File(destDir, "elements.json").writeText(elementsSrc.toURL().readText())

        val elementCommentsSrc = URI("https://static.btcmap.org/api/v3/element-comments.json")
        File(destDir, "element-comments.json").writeText(elementCommentsSrc.toURL().readText())

        val reportsSrc = URI("https://static.btcmap.org/api/v3/reports.json")
        File(destDir, "reports.json").writeText(reportsSrc.toURL().readText())

        val eventsSrc = URI("https://static.btcmap.org/api/v3/events.json")
        File(destDir, "events.json").writeText(eventsSrc.toURL().readText())

        val areasSrc = URI("https://static.btcmap.org/api/v3/areas.json")
        File(destDir, "areas.json").writeText(areasSrc.toURL().readText())

        val areaElementsSrc = URI("https://static.btcmap.org/api/v3/area-elements.json")
        File(destDir, "area-elements.json").writeText(areaElementsSrc.toURL().readText())

        val usersSrc = URI("https://static.btcmap.org/api/v3/users.json")
        File(destDir, "users.json").writeText(usersSrc.toURL().readText())
    }
}