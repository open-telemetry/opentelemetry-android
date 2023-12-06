import io.opentelemetry.gradle.OtelAndroidVersionClassPlugin

plugins {
    id("com.android.library")
    id("otel.errorprone-conventions")
}

apply<OtelAndroidVersionClassPlugin>()

android {
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
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

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(libs.findLibrary("findbugs-jsr305").get())
    // Workaround for @javax.annotation.Generated
    // see: https://github.com/grpc/grpc-java/issues/3633
    // Required for generated OtelAndroidVersion.java
    compileOnly(libs.findLibrary("javax-annotation-api").get())
}