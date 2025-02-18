rootProject.name = "opentelemetry-android"

plugins {
    id("com.gradle.develocity") version "3.19.2"
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI") != null }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

include(":core")
include(":android-agent")
include(":instrumentation:activity")
include(":instrumentation:anr")
include(":instrumentation:common-api")
include(":instrumentation:crash")
include(":instrumentation:fragment")
include(":instrumentation:okhttp:okhttp-3.0:agent")
include(":instrumentation:okhttp:okhttp-3.0:library")
include(":instrumentation:okhttp:okhttp-3.0:testing")
include(":instrumentation:network")
include(":instrumentation:sessions")
include(":instrumentation:slowrendering")
include(":instrumentation:startup")
include(":instrumentation:volley:library")
include(":instrumentation:httpurlconnection:agent")
include(":instrumentation:httpurlconnection:library")
include(":instrumentation:httpurlconnection:testing")
include(":test-common")
include(":animal-sniffer-signature")
include(":instrumentation:android-instrumentation")
include(":common")
include(":services")
include(":session")
include(":opentelemetry-android-bom")
