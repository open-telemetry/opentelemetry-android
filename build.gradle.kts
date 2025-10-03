import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    alias(libs.plugins.publishPlugin)
    id("org.jetbrains.kotlinx.kover")
}

extra["java_version"] = JavaVersion.VERSION_1_8
extra["jvm_target"] = JvmTarget.JVM_1_8
extra["kotlin_min_supported_version"] = KotlinVersion.KOTLIN_1_8

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

nexusPublishing {
    repositories {
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_KEY"))
        }
    }
}

kover {
    merge {
        subprojects { project ->
            true
        }
    }
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                classes("*.BuildConfig")
            }
        }
    }
}
