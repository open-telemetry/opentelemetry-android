import org.gradle.api.publish.maven.MavenPublication
import java.net.URI

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    lint {
        warningsAsErrors = true
        // A newer version of androidx.appcompat:appcompat than 1.3.1 is available: 1.4.1 [GradleDependency]
        // we rely on dependabot for dependency updates
        disable.add("GradleDependency")
    }
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
            groupId = "io.opentelemetry.android"
            artifactId = base.archivesName.get()

            afterEvaluate {
                pom.name.set("${project.extra["pomName"]}")
                pom.description.set(project.description)
            }

            pom {
                url.set("https://github.com/open-telemetry/opentelemetry-android")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("opentelemetry")
                        name.set("OpenTelemetry Authors")
                        email.set("support+java@opentelemetry.io")
                        organization.set("OpenTelemetry")
                        organizationUrl.set("https://opentelemetry.io")
                    }
                }
                scm {
                    connection.set("https://github.com/open-telemetry/opentelemetry-android.git")
                    developerConnection.set("https://github.com/open-telemetry/opentelemetry-android.git")
                    url.set("https://github.com/open-telemetry/opentelemetry-android")
                }
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

val sourcesJar by tasks.registering(Jar::class) {

    from(android.sourceSets.named("main").get().java.srcDirs)
    archiveClassifier.set("sources")
}

project.afterEvaluate {

    // note: we need to declare this here in afterEvaluate because the android plugin doesn't
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
