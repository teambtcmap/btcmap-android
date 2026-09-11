import java.net.URI
import java.util.Properties
import org.gradle.api.GradleException

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

val bundleData = tasks.register<DefaultTask>("bundleData") {
    outputs.file(File(projectDir, "src/main/assets/bundled-places.json"))
    outputs.upToDateWhen { false }
    doLast {
        val dir = File(projectDir, "src/main/assets")
        dir.mkdirs()
        val connection = URI("https://api.btcmap.org/v4/places?fields=id,lat,lon,icon,name,comments,boosted_until")
            .toURL()
            .openConnection()
            .apply {
                connectTimeout = 30_000
                readTimeout = 60_000
            }
        val data = connection.getInputStream().bufferedReader().use { it.readText() }
        File(dir, "bundled-places.json").writeText(data)
    }
}

tasks.configureEach {
    if (name != "bundleData") {
        mustRunAfter(bundleData)
    }
}

tasks.register<DefaultTask>("bundleMapStyles") {
    val assetsDir = File(projectDir, "src/main/assets/map-styles")
    outputs.dir(assetsDir)
    doLast {
        val script = File(projectDir, "bundle_map_styles.py")
        if (!script.exists()) {
            throw GradleException("Missing bundler script at $script")
        }
        val args = mutableListOf("python3", script.absolutePath)
        if (project.hasProperty("force")) {
            args += "--force"
        }
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        println(output)
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("bundle_map_styles.py failed with exit code $exitCode")
        }
    }
}