plugins {
    id("otel.android-app-conventions")
}

android {
    namespace = "com.example.hello_otel"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

}

dependencies {
    api(libs.timber)
    api(libs.opentelemetry.sdk.testing)
    api(libs.opentelemetry.exporter.otlp)
    api(libs.opentelemetry.exporter.logging)
    api(libs.opentelemetry.extension.trace.propagators)
    api(libs.okhttp.mockwebserver)
    api(project(":android-agent"))
    api(libs.annotationx)
    api(libs.retrofit)
    api(libs.converter.gson)
    api(libs.retrofit2.adapter.rxjava2)
    api(libs.rxjava)
    api(libs.rxandroid)
    api(libs.gson)
    api(libs.rxrelay)
    implementation(libs.autodispose)
    implementation(libs.autodispose.android.archcomponents)
    implementation(libs.autodispose.android)



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(project(":instrumentation:okhttp:okhttp-3.0:library"))
    debugImplementation (libs.chuck.library)
    releaseImplementation (libs.chuck.library.no.op)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.truth)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.truth)


}
