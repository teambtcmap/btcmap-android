buildscript {
    dependencies {
        // Overrides the Kotlin version bundled with AGP's built-in Kotlin support
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
