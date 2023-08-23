import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

val isARelease = project.hasProperty("release") && project.property("release") == "true"

val variantToPublish = "release"
android {
    lint {
        warningsAsErrors = true
        // A newer version of androidx.appcompat:appcompat than 1.3.1 is available: 1.4.1 [GradleDependency]
        // we rely on dependabot for dependency updates
        disable.add("GradleDependency")
    }

    publishing {
        singleVariant(variantToPublish) {

            // Adding sources and javadoc artifacts only during a release.
            if (isARelease) {
                withJavadocJar()
                withSourcesJar()
            }
        }
    }
}

afterEvaluate {
    publishing.publications {
        val maven = create<MavenPublication>("maven") {
            from(components.findByName(variantToPublish))
            pom {
                val repoUrl = "https://github.com/open-telemetry/opentelemetry-android"
                name.set("OpenTelemetry Android")
                description.set(project.description)
                url.set(repoUrl)
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    val scmUrl = "scm:git:git@github.com:open-telemetry/opentelemetry-android.git"
                    connection.set(scmUrl)
                    developerConnection.set(scmUrl)
                    url.set(repoUrl)
                    tag.set("HEAD")
                }
                developers {
                    developer {
                        id.set("opentelemetry")
                        name.set("OpenTelemetry")
                        url.set("https://github.com/open-telemetry/community")
                    }
                }
            }
        }

        // Signing only during a release.
        if (isARelease) {
            signing {
                useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PASSWORD"))
                sign(maven)
            }
        }
    }
}