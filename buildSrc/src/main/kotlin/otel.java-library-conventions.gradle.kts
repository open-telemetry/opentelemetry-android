import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("otel.errorprone-conventions")
    id("ru.vyarus.animalsniffer")
}

// Extension to configure android parameters for non-android projects.
interface OtelAndroidExtension {
    val minSdk: Property<Int>
}

val otelAndroidExtension =
    project.extensions.create("otelAndroid", OtelAndroidExtension::class.java)
otelAndroidExtension.minSdk.convention((project.property("android.minSdk") as String).toInt())

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val minKotlinVersion = rootProject.extra["kotlin_min_supported_version"] as KotlinVersion
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        apiVersion.set(minKotlinVersion)
        languageVersion.set(minKotlinVersion)
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(libs.findLibrary("findbugs-jsr305").get())
    compileOnly(libs.findLibrary("auto-service-annotations").get())
    kapt(libs.findLibrary("auto-service-processor").get())
}

animalsniffer {
    sourceSets = listOf(java.sourceSets.main.get())
}

// Always having declared output makes this task properly participate in tasks up-to-date checks
tasks.withType<AnimalSniffer> {
    reports.text.required.set(true)
}

// Attaching animalsniffer check to the compilation process.
tasks.named("classes").configure {
    finalizedBy("animalsnifferMain")
}

afterEvaluate {
    dependencies {
        signature("com.toasttab.android:gummy-bears-api-${otelAndroidExtension.minSdk.get()}:0.5.1:coreLib@signature")
    }
}