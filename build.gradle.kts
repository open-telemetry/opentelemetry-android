// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // keep this version in sync with /buildSrc/build.gradle.kts
        classpath("com.android.tools.build:gradle:8.1.2")
        classpath("net.bytebuddy:byte-buddy-gradle-plugin:${property("bytebuddy.version")}")
    }
}

plugins {
    id("otel.spotless-conventions")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

extra["java_version"] = JavaVersion.VERSION_1_8

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
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
