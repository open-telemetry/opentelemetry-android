plugins {
    id("java-platform")
    id("otel.publish-conventions")
}

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
