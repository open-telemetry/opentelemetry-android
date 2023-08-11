plugins {
    `kotlin-dsl`

    id("java-gradle-plugin")
    // When updating, update below in dependencies too
    id("com.diffplug.spotless") version "6.20.0"
}

spotless {
    kotlinGradle {
        ktlint()
        target("* * / *.gradle.kts")
    }
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    // keep this version in sync with /build.gradle.kts
    implementation("com.android.tools.build:gradle:8.1.0")

    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.20.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin:1.6.0")
}

gradlePlugin {
    plugins {
        create("publishPlugin") {
            id = "otel.android.publishing"
            implementationClass = "io.opentelemetry.android.PublishPlugin"
        }
    }
}
