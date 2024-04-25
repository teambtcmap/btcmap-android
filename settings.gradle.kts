pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        // TODO get rid of it. It's still needed for charts and bundled SQLite
        maven("https://jitpack.io")
    }
}

rootProject.name = "BTC Map"
include(":app")