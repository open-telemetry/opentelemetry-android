import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        googleJavaFormat().aosp()
        licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public)")
        target("src/**/*.java")
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        configureKotlin(this@spotless)
    }
    plugins.withId("org.jetbrains.kotlin.android") {
        configureKotlin(this@spotless)
    }
    kotlinGradle {
        ktlint()
    }
    format("misc") {
        // not using "**/..." to help keep spotless fast
        target(
                ".gitignore",
                ".gitattributes",
                ".gitconfig",
                ".editorconfig",
                "*.md",
                "src/**/*.md",
                "docs/**/*.md",
                "*.sh",
                "src/**/*.properties"
        )
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Use root declared tool deps to avoid issues with high concurrency.
// see https://github.com/diffplug/spotless/tree/main/plugin-gradle#dependency-resolution-modes
if (project == rootProject) {
    spotless {
        predeclareDeps()
    }
    with(extensions["spotlessPredeclare"] as SpotlessExtension) {
        java {
            googleJavaFormat()
        }
        kotlin {
            ktlint()
        }
        kotlinGradle {
            ktlint()
        }
    }
}

fun configureKotlin(
    spotlessExtension: SpotlessExtension,
) {
    spotlessExtension.kotlin {
        ktlint()
        licenseHeaderFile(
            rootProject.file("gradle/spotless.license.java")
        )
        target("src/**/*.kt")
    }
}