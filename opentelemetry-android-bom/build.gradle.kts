plugins {
    id("java-platform")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Bill of Materials"

dependencies {
    constraints {
        rootProject.subprojects.forEach { subproject ->
            if (subproject != project) {
                subproject.plugins.withId("maven-publish") {
                    api(subproject)
                }
            }
        }
    }
}
