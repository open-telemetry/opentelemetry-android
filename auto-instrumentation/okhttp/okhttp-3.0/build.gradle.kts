plugins {
    id("java-library")
}

java {
    val javaVersion = rootProject.extra["java_version"] as JavaVersion
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}