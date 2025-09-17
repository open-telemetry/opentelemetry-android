import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("otel.errorprone-conventions")
    id("com.google.devtools.ksp")
    id("otel.animalsniffer-conventions")
    id("otel.android-dependency-conventions")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("org.jetbrains.kotlinx.kover")
    id("otel.spotless-conventions")
}

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val targetJvm = rootProject.extra["jvm_target"] as JvmTarget
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

    kotlin {
        compilerOptions {
            jvmTarget.set(targetJvm)
            apiVersion.set(minKotlinVersion)
            languageVersion.set(minKotlinVersion)
            freeCompilerArgs.set(listOf("-Xjvm-default=all"))
        }
    }
    testOptions {
        unitTests {
            all { test ->
                test.maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2) + 1
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    autoCorrect = true

    // overwrite default behaviour here, if needed
    config.from(rootProject.files("config/detekt/detekt.yml"))

    // suppress pre-existing issues on a per-project basis
    baseline = project.file("config/detekt/baseline.xml")
}

project.tasks.withType(Detekt::class.java).configureEach {
    jvmTarget = targetJvm.target
    reports {
        html.required.set(true)
        xml.required.set(false)
    }
}

// disable kotlin's binary compat validator for unwanted modules
val ignoredModules = listOf("test-common")
apiValidation.validationDisabled = ignoredModules.contains(project.name)

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(libs.findLibrary("androidx-annotation").get())
    implementation(libs.findLibrary("findbugs-jsr305").get())
    implementation(libs.findLibrary("auto-service-annotations").get())
    ksp(libs.findLibrary("auto-service-processor").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findBundle("mocking").get())
    testImplementation(libs.findBundle("junit").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testImplementation(libs.findLibrary("opentelemetry-sdk-testing").get())
    testImplementation(libs.findLibrary("androidx-junit").get())
}