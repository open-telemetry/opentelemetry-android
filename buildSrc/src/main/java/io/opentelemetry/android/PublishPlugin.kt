package io.opentelemetry.android

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.LibrarySingleVariant
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPomLicense
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPomScm
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

/**
 * This plugin takes care of configuring a library project by creating a maven publication that
 * contains the library artifact and (when the "-Prelease=true" param is present in the gradle command) it will also include
 * the library's javadoc, sources artifacts and all the artifacts will be signed.
 */
class PublishPlugin : Plugin<Project> {
    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
        val plugins = project.plugins
        applyPublishingPlugins(plugins)
        plugins.withId("com.android.library") {
            configureAndroidLibraryPublication()
        }
    }

    private fun applyPublishingPlugins(plugins: PluginContainer) {
        plugins.apply("maven-publish")
        plugins.apply("signing")
    }

    private fun configureAndroidLibraryPublication() {
        val androidExtension = project.extensions.getByType(
            LibraryExtension::class.java
        )
        val variantToRelease = "release"
        androidExtension.publishing.singleVariant(variantToRelease) {
            if (isARelease()) {
                withJavadocJar()
                withSourcesJar()
            }
        }
        project.afterEvaluate {
            addMavenPublication(variantToRelease)
        }
    }

    private fun addMavenPublication(componentName: String) {
        val mavenPublishExtension = project.extensions.getByType(
            PublishingExtension::class.java
        )
        mavenPublishExtension.publications.create<MavenPublication>(
            "maven",
            MavenPublication::class.java
        ) {
            from(project.components.findByName(componentName))
            configurePom(this)
            if (isARelease()) {
                signPublication(this)
            }
        }
    }

    private fun configurePom(publication: MavenPublication) {
        publication.pom {
            val repoUrl = "https://github.com/open-telemetry/opentelemetry-android"
            name.set(project.group.toString() + ":" + project.name)
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

    private fun signPublication(publication: MavenPublication) {
        val signing = project.extensions.getByType(
            SigningExtension::class.java
        )
        signing.sign(publication)
    }

    private fun isARelease(): Boolean {
        val propertyName = "release"
        if (!project.hasProperty(propertyName)) {
            return false
        }
        val release = project.property(propertyName) as? String
        return release?.lowercase() == "true"
    }
}