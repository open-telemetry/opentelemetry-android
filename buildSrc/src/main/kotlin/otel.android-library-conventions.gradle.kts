import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("otel.errorprone-conventions")
}

android {
    defaultConfig {
        minSdk = (project.property("android.minSdk") as String).toInt()
    }

    lint {
        warningsAsErrors = true
        // A newer version of androidx.appcompat:appcompat than 1.3.1 is available: 1.4.1 [GradleDependency]
        // we rely on dependabot for dependency updates
        disable.add("GradleDependency")
    }

    compileOptions {
        val javaVersion = rootProject.extra["java_version"] as JavaVersion
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }
}