plugins {
    `java-platform`
}

val otelSdkVersion = "1.30.1"
rootProject.extra["otelSdkVersion"] = otelSdkVersion
val otelSdkAlphaVersion = otelSdkVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelInstrumentationVersion = "1.30.0"
val otelInstrumentationAlphaVersion =
    otelInstrumentationVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")

val androidTestRunnerVersion = "1.5.2"
rootProject.extra["androidTestRunnerVersion"] = androidTestRunnerVersion
val androidxCoreVersion = "1.12.0"
val androidxNavFragmentVersion = "2.7.3"
val androidxTestCoreVersion = "1.5.0"
val appCompatVersion = "1.6.1"
val asmVersion = "9.5"
val assertjVersion = "3.24.2"
val awaitilityVersion = "4.2.0"
val byteBuddyVersion = "1.14.8"
val errorProneVersion = "2.21.1"
val jmhVersion = "1.37"
val junitVersion = "5.10.0"
val mockitoVersion = "5.5.0"
val mockwebserverVersion = "4.11.0"

val okhttpVersion = "4.11.0"
rootProject.extra["okhttpVersion"] = okhttpVersion
val roboelectricVersion = "4.10.3"
val slf4jVersion = "2.0.9"

javaPlatform {
    allowDependencies()
}

dependencies {

    // BOMs
    api(enforcedPlatform("org.junit:junit-bom:$junitVersion"))
    api(enforcedPlatform("io.opentelemetry:opentelemetry-bom-alpha:$otelSdkAlphaVersion"))
    api(enforcedPlatform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelInstrumentationAlphaVersion"))
    api(platform("com.squareup.okhttp3:okhttp-bom:$okhttpVersion"))

    constraints {
        api("androidx.appcompat:appcompat:$appCompatVersion")
        api("androidx.core:core:$androidxCoreVersion")
        api("androidx.navigation:navigation-fragment:$androidxNavFragmentVersion")

        api("androidx.test:runner:$androidTestRunnerVersion")
        api("androidx.test:core:$androidxTestCoreVersion")
        api("org.awaitility:awaitility:$awaitilityVersion")
        api("org.assertj:assertj-core:$assertjVersion")
        api("org.mockito:mockito-core:$mockitoVersion")
        api("org.mockito:mockito-junit-jupiter:$mockitoVersion")
        api("org.robolectric:robolectric:$roboelectricVersion")
        api("com.squareup.okhttp3:mockwebserver:$mockwebserverVersion")
    }
}
