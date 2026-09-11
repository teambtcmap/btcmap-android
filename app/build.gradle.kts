import com.google.gson.JsonParser
import java.net.HttpURLConnection
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

// Downloads the bundled places snapshot into a git-ignored asset. This task is
// intentionally not wired into assembleRelease/assembleBeta, so packaging must
// run bundleData explicitly to ship an offline snapshot with the APK.
val bundleData = tasks.register<DefaultTask>("bundleData") {
    val outputFile = File(projectDir, "src/main/assets/bundled-places.json")
    outputs.file(outputFile)
    outputs.upToDateWhen { false }
    doLast {
        val outputDir = outputFile.parentFile
        outputDir.mkdirs()

        val url = URI("https://api.btcmap.org/v4/places?fields=id,lat,lon,icon,name,comments,boosted_until")
            .toURL()
        val connection = url.openConnection() as HttpURLConnection
        val body = try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BTC Map Android ${android.defaultConfig.versionCode}")
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw GradleException("Failed to download bundled places: HTTP $code ${connection.responseMessage}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val places = try {
            JsonParser.parseString(body)
        } catch (e: Exception) {
            throw GradleException("Downloaded bundled places are not valid JSON", e)
        }
        if (!places.isJsonArray || places.asJsonArray.size() == 0) {
            throw GradleException("Downloaded bundled places are empty or not a JSON array")
        }

        // Write atomically so a failed download never leaves a truncated asset.
        val tmpFile = File(outputDir, "${outputFile.name}.tmp")
        tmpFile.writeText(body)
        if (!tmpFile.renameTo(outputFile)) {
            tmpFile.copyTo(outputFile, overwrite = true)
            tmpFile.delete()
        }

        println("Bundled ${places.asJsonArray.size()} places into ${outputFile.relativeTo(rootProject.projectDir)}")
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
    outputs.upToDateWhen { false }
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