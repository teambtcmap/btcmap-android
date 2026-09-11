buildscript {
    dependencies {
        // Overrides the Kotlin version bundled with AGP's built-in Kotlin support
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        // Used by the app build script to validate downloaded JSON
        classpath("com.google.code.gson:gson:${libs.versions.gson.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
