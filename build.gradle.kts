buildscript {
    repositories {
        maven("https://repo.maven.apache.org/maven2/")
        google() // Direct URL is not supported by F-Droid
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
    }
}

allprojects {
    repositories {
        maven("https://repo.maven.apache.org/maven2/")
        google() // Direct URL is not supported by F-Droid
        maven("https://jitpack.io")
        mavenCentral()
    }
}

task("clean") {
    delete(rootProject.buildDir)
}
