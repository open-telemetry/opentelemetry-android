import gradle.kotlin.dsl.accessors._98848321c7233c2f7fb697478033dcb2.implementation
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

dependencies {
    implementation(platform(project(":dependencyManagement")))
    signature("com.toasttab.android:gummy-bears-api-${project.property("android.minSdk")}:0.5.1:coreLib@signature")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
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
