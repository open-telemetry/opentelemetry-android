plugins {
    id("otel.android-app-conventions")
}

android {
    namespace = "io.opentelemetry.android.smoketestapp"

    defaultConfig {
        applicationId = "io.opentelemetry.android.smoketestapp"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":android-agent"))
}
