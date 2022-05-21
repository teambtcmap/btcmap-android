buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.4.2")
        classpath("com.squareup.sqldelight:gradle-plugin:1.5.3")
    }
}

allprojects {
    repositories {
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://jitpack.io")
        maven("https://dl.google.com/dl/android/maven2/")
    }
}

task("clean") {
    delete(rootProject.buildDir)
}