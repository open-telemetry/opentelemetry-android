import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("otel.errorprone-conventions")
    id("kotlin-kapt")
    id("otel.animalsniffer-conventions")
}

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val minKotlinVersion = rootProject.extra["kotlin_min_supported_version"] as KotlinVersion
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
        disable.add("AndroidGradlePluginVersion")
        disable.add("NewApi")
    }

    compileOptions {
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
        apiVersion = minKotlinVersion.version
        languageVersion = minKotlinVersion.version
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(libs.findLibrary("androidx-annotation").get())
    implementation(libs.findLibrary("findbugs-jsr305").get())
    implementation(libs.findLibrary("auto-service-annotations").get())
    kapt(libs.findLibrary("auto-service-processor").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findBundle("mocking").get())
    testImplementation(libs.findBundle("junit").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testImplementation(libs.findLibrary("opentelemetry-sdk-testing").get())
    testImplementation(libs.findLibrary("androidx-junit").get())
}