import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
    id("java-library")
    id("otel.errorprone-conventions")
    id("ru.vyarus.animalsniffer")
}

java {
    val javaVersion = rootProject.extra["java_version"] as JavaVersion
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    signature("com.toasttab.android:gummy-bears-api-${project.property("android.minSdk")}:0.5.1:coreLib@signature")
    implementation(libs.findLibrary("findbugs-jsr305").get())
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
