plugins {
    id("java-platform")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Bill of Materials"

javaPlatform.allowDependencies()

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(platform(libs.opentelemetry.platform))
    api(platform(libs.opentelemetry.core.bom))
    rootProject.subprojects.forEach { subproject ->
        if (subproject != project) {
            subproject.plugins.withId("maven-publish") {
                api(subproject)
            }
        }
    }
}
