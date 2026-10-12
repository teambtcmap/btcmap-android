pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // The Conveyor packaging plugin. It is not on the Gradle Plugin Portal's
        // own repository, only its marker is; the plugin itself lives here.
        maven("https://maven.hq.hydraulic.software")
    }
}

plugins {
    // Provides the JDK requested by gradle/gradle-daemon-jvm.properties
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BTC Map"

include(":app")
include(":shared")
include(":ui")
include(":desktopApp")
