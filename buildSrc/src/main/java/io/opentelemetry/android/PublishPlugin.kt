package io.opentelemetry.android;

import com.android.build.api.dsl.LibraryExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.plugins.signing.SigningExtension;

import java.util.Locale;

import kotlin.Unit;

/**
 * This plugin takes care of configuring a library project by creating a maven publication that
 * contains the library artifact and (when the "-Prelease=true" param is present in the gradle command) it will also include
 * the library's javadoc, sources artifacts and all the artifacts will be signed.
 */
public class PublishPlugin implements Plugin<Project> {
    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;
        PluginContainer plugins = project.getPlugins();
        applyPublishingPlugins(plugins);
        plugins.withId("com.android.library", plugin -> {
            configureAndroidLibraryPublication();
        });
    }

    private void applyPublishingPlugins(PluginContainer plugins) {
        plugins.apply("maven-publish");
        plugins.apply("signing");
    }

    private void configureAndroidLibraryPublication() {
        LibraryExtension androidExtension = project.getExtensions().getByType(LibraryExtension.class);
        String variantToRelease = "release";
        androidExtension.getPublishing().singleVariant(variantToRelease, librarySingleVariant -> {
            if (isARelease()) {
                librarySingleVariant.withJavadocJar();
                librarySingleVariant.withSourcesJar();
            }
            return Unit.INSTANCE;
        });

        project.afterEvaluate(self -> addMavenPublication(variantToRelease));
    }

    private void addMavenPublication(String componentName) {
        PublishingExtension mavenPublishExtension = project.getExtensions().getByType(PublishingExtension.class);
        mavenPublishExtension.getPublications().create("maven", MavenPublication.class, publication -> {
            publication.from(project.getComponents().findByName(componentName));
            configurePom(publication);
            if (isARelease()) {
                signPublication(publication);
            }
        });
    }

    private void configurePom(MavenPublication publication) {
        publication.pom(mavenPom -> {
            String repoUrl = "https://github.com/open-telemetry/opentelemetry-android";
            mavenPom.getName().set(project.getGroup() + ":" + project.getName());
            mavenPom.getDescription().set(project.getDescription());
            mavenPom.getUrl().set(repoUrl);
            mavenPom.licenses(spec -> spec.license(license -> {
                license.getName().set("The Apache Software License, Version 2.0");
                license.getUrl().set("http://www.apache.org/licenses/LICENSE-2.0.txt");
            }));
            mavenPom.scm(scm -> {
                String scmUrl = "scm:git:git@github.com:open-telemetry/opentelemetry-android.git";
                scm.getConnection().set(scmUrl);
                scm.getDeveloperConnection().set(scmUrl);
                scm.getUrl().set(repoUrl);
                scm.getTag().set("HEAD");
            });
            mavenPom.developers(spec -> spec.developer(developer -> {
                developer.getId().set("opentelemetry");
                developer.getName().set("OpenTelemetry");
                developer.getUrl().set("https://github.com/open-telemetry/community");
            }));
        });
    }

    private void signPublication(MavenPublication publication) {
        SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);
        signing.sign(publication);
    }

    /**
     * Here we check if the parameter "release=true" was passed in the Gradle command, e.g: "./gradlew assemble -Prelease=true"
     * if so then it means that the compiled artifacts will include aditional files, such as the
     * javadoc and sources artifacts, and all of the artifacts will be signed.
     * <p>
     * The reason why the extra artifacts and the signing process is not done by default is because
     * the generation of extra artifacts adds more time to the compilation process so it's better
     * to avoid doing that work during development compilations. Also, signing the artifacts requires
     * a set of secret parameters that should only be available within a CI/CD pipeline.
     */
    private boolean isARelease() {
        String propertyName = "release";
        if (!project.hasProperty(propertyName)) {
            return false;
        }
        String release = (String) project.property(propertyName);
        return release.toLowerCase(Locale.US).equals("true");
    }
}
