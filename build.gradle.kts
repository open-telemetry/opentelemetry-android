// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // keep this version in sync with /buildSrc/build.gradle.kts
        classpath("com.android.tools.build:gradle:8.1.0")
    }
}

plugins {
    id("otel.spotless-conventions")
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
    if (findProperty("release") != "true") {
        version = "$version-SNAPSHOT"
    }
}

subprojects {
    apply(plugin = "otel.spotless-conventions")
}
