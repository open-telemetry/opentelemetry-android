plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

//android {
//    testOptions.unitTests.includeAndroidResources = true
//}

dependencies {
    byteBuddy(project(":instrumentation:okhttp:okhttp-3.0:agent"))
    implementation(project(":instrumentation:okhttp:okhttp-3.0:library"))
    implementation(libs.okhttp)
    api(libs.annotationx)
    api(libs.opentelemetry.sdk.testing)
    implementation(libs.opentelemetry.exporter.otlp)

    androidTestImplementation(libs.gson)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.okhttp.mockwebserver)

    testImplementation(libs.gson)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.opentelemetry.api)
    testImplementation(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.instrumentation.apiSemconv)
    testImplementation("junit:junit:4.13.2")

}
