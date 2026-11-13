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
        mavenLocal {
            // Only the snapshots under test, so nothing else in Maven Local shadows a release.
            content {
                includeGroupByRegex("io\\.opentelemetry\\.android(\\..*)?")
            }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files(providers.gradleProperty("catalogPath").get()))
        }
    }
}

rootProject.name = "consumer-app"
