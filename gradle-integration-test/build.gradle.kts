plugins {
    id("org.jetbrains.kotlin.jvm")
    id("otel.spotless-conventions")
}

description = "Verifies the published artifacts are consumable at the minimum supported toolchain"

val minSupportedJdk = libs.versions.minSupportedJdk.get()

// TestKit starts the fixture builds on the JVM that runs the tests, so this is the consumer's JDK.
kotlin {
    jvmToolchain(minSupportedJdk.toInt())
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val bomVersion = "${project.version}-alpha-SNAPSHOT"

val fixtures = layout.projectDirectory.dir("src/test/resources/fixtures")
val catalog = rootProject.layout.projectDirectory.file("gradle/libs.versions.toml")

val minSdk = project.property("android.minSdk") as String
val minCompileSdk = project.property("android.minCompileSdk") as String

val androidSdkDir: Provider<String> =
    providers
        .environmentVariable("ANDROID_HOME")
        .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
        .orElse(
            providers
                .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
                .asText
                .map { contents ->
                    contents
                        .lineSequence()
                        .firstOrNull { it.startsWith("sdk.dir=") }
                        ?.substringAfter("sdk.dir=")
                        ?.replace("\\:", ":")
                        ?.trim()
                        .orEmpty()
                },
        ).orElse("")

tasks.test {
    enabled = false
}

tasks.register<Test>("minSupportedVersionsTest") {
    description = "Builds consumer apps against the mavenLocal snapshots at the minimum supported versions."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    systemProperty("fixtureSrcDir", fixtures.asFile.absolutePath)
    systemProperty("catalogPath", catalog.asFile.absolutePath)
    systemProperty("bomVersion", bomVersion)
    systemProperty("minSupportedGradle", libs.versions.minSupportedGradle.get())
    systemProperty("minSupportedJdk", minSupportedJdk)
    systemProperty("minSdk", minSdk)
    systemProperty("minCompileSdk", minCompileSdk)
    systemProperty("androidSdkDir", androidSdkDir.get())

    doNotTrackState("Consumes artifacts from mavenLocal, which Gradle does not track")

    testLogging {
        showStandardStreams = true
        showStackTraces = false
    }
}
