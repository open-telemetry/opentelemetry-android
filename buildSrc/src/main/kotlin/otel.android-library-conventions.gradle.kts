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
        }
    }
}
