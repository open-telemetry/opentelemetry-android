plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android aggregate: all available instrumentation features"

android {
    namespace = "io.opentelemetry.android.instrumentation.all"
    
    defaultConfig {
        // No consumer rules; this module just aggregates others.
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    // Core/common building blocks some instrumentations rely on transitively
    api(project(":instrumentation:common-api"))
    api(project(":instrumentation:android-instrumentation"))

    // Feature instrumentations (add new ones here as they are added to the repo)
    api(project(":instrumentation:activity"))
    api(project(":instrumentation:anr"))
    api(project(":instrumentation:crash"))
    api(project(":instrumentation:fragment"))
    api(project(":instrumentation:network"))
    api(project(":instrumentation:slowrendering"))
    api(project(":instrumentation:sessions"))
    api(project(":instrumentation:startup"))
    api(project(":instrumentation:view-click"))
    // Compose specific (click tracking)
    api(project(":instrumentation:compose:click"))
    // HTTP client / networking related (OkHttp3, WebSocket, HttpURLConnection)
    api(project(":instrumentation:okhttp3:library"))
    api(project(":instrumentation:okhttp3-websocket:library"))
    api(project(":instrumentation:httpurlconnection:library"))
    // Android log instrumentation (library flavor)
    api(project(":instrumentation:android-log:library"))

    // SDK / internal dependencies required by some features
    implementation(project(":services"))
    implementation(project(":session"))
    implementation(project(":common"))
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
}
