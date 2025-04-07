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
            val path = project.path
            artifactId = computeArtifactId(path)
            groupId = computeGroupId(path)
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

fun computeArtifactId(path: String): String {
    val projectName = project.name
    if (!path.startsWith(":instrumentation:")) {
        // Return default artifactId for non auto-instrumentation publications.
        return projectName
    }

    val match = Regex("^:instrumentation:([^:]+)(:[^:]+)?\$").matchEntire(path)
        ?: throw IllegalStateException("Invalid instrumentation path: '$path'")

    if (match.groupValues[2].isEmpty()) {
        // The instrumentation has no subprojects
        return projectName
    }

    val instrumentationName = match.groupValues[1].replace(":", "")
    val instrumentationSubprojectName = match.groupValues[2].replace(":", "")

    // Adding instrumentation name to its related subprojects.
    // For example, prepending "okhttp-" to both the "library" and "agent" subprojects inside the "okhttp" folder.
    val artifactId = "$instrumentationName-$instrumentationSubprojectName"

    logger.debug("Using artifact id: '{}' for subproject: '{}'", artifactId, path)
    return artifactId
}

fun computeGroupId(path: String): String {
    val groupId = project.group.toString()
    if (!path.startsWith(":instrumentation:")) {
        // Return default artifactId for non auto-instrumentation publications.
        return groupId
    }

    return "$groupId.instrumentation"
}