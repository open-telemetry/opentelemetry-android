import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.plugin)
        classpath(libs.byteBuddy.plugin)
    }
}

plugins {
    id("otel.spotless-conventions")
    alias(libs.plugins.publishPlugin)
}

extra["java_version"] = JavaVersion.VERSION_1_8
extra["kotlin_min_supported_version"] = KotlinVersion.KOTLIN_1_8

allprojects {
    repositories {
        google()
        mavenCentral()
    }
    if (findProperty("final") != "true") {
        version = "$version-SNAPSHOT"
    }
}

subprojects {
    apply(plugin = "otel.spotless-conventions")
}

nexusPublishing.repositories {
    sonatype {
        username.set(System.getenv("SONATYPE_USER"))
        password.set(System.getenv("SONATYPE_KEY"))
    }
}
