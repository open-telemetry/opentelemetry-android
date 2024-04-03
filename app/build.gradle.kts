plugins {
    id("otel.android-app-conventions")
}

android {
    namespace = "com.example.app"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

}
