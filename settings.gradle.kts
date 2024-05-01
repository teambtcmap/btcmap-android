// Based on Android Studio new app template
// The only difference is the inclusion of jitpack (TODO remove)

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