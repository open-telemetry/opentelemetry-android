import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

extensions.configure<SpotlessExtension>("spotless") {
    java {
        googleJavaFormat().aosp()
        licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public)")
        target("src/**/*.java")
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        configureKotlin(this@configure)
    }
    plugins.withId("org.jetbrains.kotlin.android") {
        configureKotlin(this@configure)
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
        indentWithSpaces()
        trimTrailingWhitespace()
        endWithNewline()
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