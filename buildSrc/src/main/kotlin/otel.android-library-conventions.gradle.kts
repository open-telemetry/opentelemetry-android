plugins {
    id("com.android.library")
    id("otel.errorprone-conventions")
}

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
        isCoreLibraryDesugaringEnabled = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(libs.findLibrary("findbugs-jsr305").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findBundle("mockito").get())
    testImplementation(libs.findBundle("junit").get())
    testImplementation(libs.findLibrary("opentelemetry-sdk-testing").get())
    coreLibraryDesugaring(libs.findLibrary("desugarJdkLibs").get())
}