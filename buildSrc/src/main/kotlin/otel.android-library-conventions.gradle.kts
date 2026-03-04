import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.validation.KotlinApiBuildTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    // Applies the kotlin-android stub defined in buildSrc so that binary-compatibility-validator's
    // withPlugin("kotlin-android") callback fires.
    // See https://youtrack.jetbrains.com/issue/KT-83410 and
    // https://issuetracker.google.com/issues/470109449.
    id("kotlin-android")
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
        aarMetadata {
            minCompileSdk = (property("android.minCompileSdk") as String).toInt()
        }
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
                test.testLogging.exceptionFormat = TestExceptionFormat.FULL
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

    // Include main and test sources (replaces variant-specific tasks removed in AGP 9)
    source.setFrom(
        "src/main/java",
        "src/main/kotlin",
        "src/test/java",
        "src/test/kotlin",
        "src/androidTest/java",
        "src/androidTest/kotlin"
    )
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

// Workaround for https://youtrack.jetbrains.com/issue/KT-83410 and
// https://issuetracker.google.com/issues/470109449.
//
// 1. AGP 9 uses KotlinBaseApiPlugin (not KotlinBasePluginWrapper) which does not populate
//    KotlinJvmAndroidCompilation.output.classesDirs. BCV therefore creates the apiBuild task
//    (triggered by our kotlin-android stub plugin) but leaves its inputClassesDirs empty,
//    causing the task to fail. Wire the release compilation output directory manually so BCV
//    has the class files it needs.
//
// 2. BCV's withKotlinPluginVersion adds kotlin-metadata-jvm to the worker classpath only for
//    kotlin-jvm and kotlin-multiplatform plugins, not for kotlin-android. Without it the
//    AbiBuildWorker fails with NoClassDefFoundError on JvmMetadataUtil. Resolve it explicitly
//    using the same KGP version that is on the compile classpath.
//
// This should be removed once the upstream KGP/AGP bug is fixed.
val kotlinMetadataForBcv: Configuration = configurations.create("kotlinMetadataForBcv") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies.add(
    kotlinMetadataForBcv.name,
    "org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.findVersion("kotlin").get().requiredVersion}"
)

tasks.withType(KotlinApiBuildTask::class.java).configureEach {
    val compileTask = tasks.named("compileReleaseKotlin", KotlinCompile::class.java)
    inputClassesDirs.from(compileTask.flatMap { it.destinationDirectory })
    runtimeClasspath.from(kotlinMetadataForBcv)
    dependsOn(compileTask)
}

dependencies {
    implementation(libs.findLibrary("androidx-annotation").get())
    implementation(libs.findLibrary("findbugs-jsr305").get())
    implementation(libs.findLibrary("auto-service-annotations").get())
    ksp(libs.findLibrary("auto-service-processor").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findBundle("mocking").get())
    testImplementation(libs.findBundle("junit").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testImplementation(platform(libs.findLibrary("opentelemetry-platform-alpha").get()))
    testImplementation(libs.findLibrary("opentelemetry-sdk-testing").get())
    testImplementation(libs.findLibrary("androidx-junit").get())
}