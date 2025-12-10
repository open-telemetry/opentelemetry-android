rootProject.name = "opentelemetry-android-demo"

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    versionCatalogs {
        create("rootLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

includeBuild("..") {
    dependencySubstitution {
        substitute(module("io.opentelemetry.android:android-agent"))
            .using(project(":android-agent"))
        substitute(module("io.opentelemetry.android:core"))
            .using(project(":core"))
        substitute(module("io.opentelemetry.android.instrumentation:compose-click"))
            .using(project(":instrumentation:compose:click"))
        substitute(module("io.opentelemetry.android.instrumentation:sessions"))
            .using(project(":instrumentation:sessions"))
    }
}