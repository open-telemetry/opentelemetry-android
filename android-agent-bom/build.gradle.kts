plugins {
    id("java-platform")
    id("otel.publish-conventions")
}

dependencies {
    constraints {
        api(project(":android-agent"))
        api(project(":common"))
        api(project(":core"))
        api(project(":services"))
        api(project(":session"))
    }
}
