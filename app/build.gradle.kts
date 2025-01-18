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
        minSdk = 27
        targetSdk = 35
        versionCode = 52
        versionName = "0.9.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }

        jniLibs {
            // Exclude all x86 lib variants
            excludes += "/lib/x86/*.so"
            excludes += "/lib/x86_64/*.so"
            // Exclude all armeabi-v7a lib variants
            excludes += "/lib/armeabi-v7a/*.so"
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

    dependencies {
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization)
        implementation(libs.kotlinx.datetime)

        implementation(libs.androidx.navigation.fragment)
        implementation(libs.androidx.navigation.ui)
        implementation(libs.androidx.work)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.room)
        implementation(libs.androidx.sqlite)

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
}
dependencies {
    implementation(libs.androidx.core.ktx)
}

tasks.register("bundleData") {
    doLast {
        val destDir = File(projectDir, "src/main/assets")
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