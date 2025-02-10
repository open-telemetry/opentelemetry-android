plugins {
    id("java-platform")
    id("otel.publish-conventions")
}

dependencies {
    constraints {
        rootProject.subprojects(
            object : Action<Project> {
                override fun execute(subproject: Project) {
                    if (subproject != project) {
                        subproject.plugins.withId("maven-publish") {
                            api(subproject)
                        }
                    }
                }
            },
        )
    }
}
