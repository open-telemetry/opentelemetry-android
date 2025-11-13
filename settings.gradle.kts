rootProject.name = "opentelemetry-android"

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI") != null }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

include(":agent-api")
include(":core")
include(":android-agent")
include(":test-common")
include(":animal-sniffer-signature")
include(":common")
include(":services")
include(":session")
include(":opentelemetry-android-bom")
includeFromDir("instrumentation")

fun includeFromDir(
    dirName: String,
    maxDepth: Int = 3,
) {
    val instrumentationDir = File(rootDir, dirName)
    val separator = Regex("[/\\\\]")
    instrumentationDir.walk().maxDepth(maxDepth).forEach {
        if (it.name.equals("build.gradle.kts")) {
            include(
                ":$dirName:${
                    it.parentFile.toRelativeString(instrumentationDir).replace(separator, ":")
                }",
            )
        }
    }
}
 