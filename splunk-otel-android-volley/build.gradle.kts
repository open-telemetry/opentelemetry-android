import java.net.URI

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    compileSdk = 31
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        all {
            //provide the version of the library as a resource so it can be used as a span attribute.
            resValue("string", "rum.version", "${project.version}")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}


dependencies {
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.navigation:navigation-fragment:2.3.5")
    compileOnly("com.android.volley:volley:1.2.0")

    api(platform("io.opentelemetry:opentelemetry-bom:1.10.0"))
    implementation("io.opentelemetry:opentelemetry-sdk")

    implementation(platform("io.opentelemetry:opentelemetry-bom-alpha:1.10.0-alpha"))
    implementation("io.opentelemetry:opentelemetry-semconv")

    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:1.10.0-alpha"))
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")

    api("io.opentelemetry:opentelemetry-api")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.robolectric:robolectric:4.7.3")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("com.google.mockwebserver:mockwebserver:20130706")
    testImplementation("com.android.volley:volley:1.2.0")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.13")

}

//TODO: create a single point of publishing configuration
val sourcesJar by tasks.registering(Jar::class) {
    from(android.sourceSets.named("main").get().java.srcDirs)
    archiveClassifier.set("sources")
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotsRepoUrl = URI("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (project.findProperty("release") == "true") releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = findProperty("mavenCentralUsername") as String?
                password = findProperty("mavenCentralPassword") as String?
            }
        }
    }
    publications {
        register<MavenPublication>("maven") {
            groupId = "com.splunk"
            artifactId = "splunk-otel-android-volley"

            pom {
                name.set("Splunk Otel Android Volley Library Instrumentation")
                description.set("A library for instrumenting Android applications using Volley Library for Splunk RUM")
                url.set("https://github.com/signalfx/splunk-otel-android")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("splunk")
                        name.set("Splunk Instrumentation Authors")
                        email.set("support+java@signalfx.com")
                        organization.set("Splunk")
                        organizationUrl.set("https://www.splunk.com")
                    }
                }
                scm {
                    connection.set("https://github.com/signalfx/splunk-otel-android.git")
                    developerConnection.set("https://github.com/signalfx/splunk-otel-android.git")
                    url.set("https://github.com/signalfx/splunk-otel-android")
                }
            }
        }
    }
}

project.afterEvaluate {

    //note: we need to declare this here in afterEvaluate because the android plugin doesn't
    // resolve dependencies early enough to make the libraryVariants hack work until here.
    val javadoc by tasks.registering(Javadoc::class) {
        source = android.sourceSets.named("main").get().java.getSourceFiles()
        classpath += project.files(android.bootClasspath)

        // grab the library variants, because apparently this is where the real classpath lives that
        // is needed for javadoc generation.
        val firstVariant = project.android.libraryVariants.toList().first()
        val javaCompile = firstVariant.javaCompileProvider.get()
        classpath += javaCompile.classpath
        classpath += javaCompile.outputs.files
    }

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn(javadoc)
        archiveClassifier.set("javadoc")
        from(javadoc.get().destinationDir)
    }

    val component = project.components.findByName("release")
    publishing {
        publications {
            named<MavenPublication>("maven") {
                from(component)
                artifact(tasks.named<Jar>("sourcesJar"))
                artifact(javadocJar)
            }
        }
    }
}

if (project.findProperty("release") == "true") {
    signing {
        useGpgCmd()
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}