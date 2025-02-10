import com.android.build.api.dsl.LibraryExtension

plugins {
    id("maven-publish")
    id("signing")
}

version = project.version.toString().replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")

val isARelease = System.getenv("CI") != null

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
    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        if (isARelease) {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing.publications {
        val maven = create<MavenPublication>("maven") {
            artifactId = computeArtifactId()
            if (android != null) {
                from(components.findByName(androidVariantToRelease))
            } else {
                val javaComponent =
                    components.findByName("java") ?: components.findByName("javaPlatform")
                javaComponent?.let {
                    from(it)
                }
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

fun computeArtifactId(): String {
    val path = project.path
    if (!path.contains("instrumentation")) {
        // Return default artifactId for non auto-instrumentation publications.
        return project.name
    }

    // Adding library name to its related auto-instrumentation subprojects.
    // For example, prepending "okhttp-3.0-" to both the "library" and "agent" subprojects inside the "okhttp-3.0" folder.
    val match = Regex("[^:]+:[^:]+\$").find(path)
    var artifactId = match!!.value.replace(":", "-")
    if (!artifactId.startsWith("instrumentation-")) {
        artifactId = "instrumentation-$artifactId"
    }

    logger.debug("Using artifact id: '{}' for subproject: '{}'", artifactId, path)
    return artifactId
}
