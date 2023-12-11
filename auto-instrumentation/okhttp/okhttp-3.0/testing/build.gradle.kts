plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android {
    buildTypes {
        debug {
            isMinifyEnabled = true
            proguardFile("proguard-rules.pro")
            testProguardFile("proguard-test-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    byteBuddy(project(":auto-instrumentation:okhttp:okhttp-3.0:agent"))
    implementation(project(":auto-instrumentation:okhttp:okhttp-3.0:library"))
    implementation(libs.okhttp)
    implementation(libs.opentelemetry.exporter.otlp)
    androidTestImplementation(libs.okhttp.mockwebserver)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}
