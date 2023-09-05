import com.android.build.api.dsl.LibraryExtension

plugins {
    id("maven-publish")
    id("signing")
}

val isARelease = project.hasProperty("release") && project.property("release") == "true"

val android = extensions.findByType(LibraryExtension::class.java)

val androidVariantToRelease = "release"
if (android != null) {
    android.publishing {
        singleVariant(androidVariantToRelease) {

            // Adding sources and javadoc artifacts only during a release.
            if (isARelease) {
                withJavadocJar()
                withSourcesJar()
            }
        }
    }
} else {
    extensions.configure(JavaPluginExtension::class.java) {
        if (isARelease) {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing.publications {
        val maven = create<MavenPublication>("maven") {
            if (android != null) {
                from(components.findByName(androidVariantToRelease))
            } else {
                from(components.findByName("java"))
            }
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
