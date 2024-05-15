import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application") version "8.4.0"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

android {
    namespace = "io.opentelemetry.android.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.opentelemetry.android.demo"
        minSdk = 21
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        all {
            val accessToken = localProperties["rum.access.token"] as String?
            resValue("string", "rum_access_token", accessToken ?: "fakebroken")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    val javaVersion = JavaVersion.VERSION_11
    compileOptions {
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

dependencies {
    implementation(demoLibs.androidx.appcompat)
    implementation(demoLibs.androidx.constraintlayout)
    implementation(demoLibs.material)
    implementation(demoLibs.androidx.lifecycle.livedata.ktx)
    implementation(demoLibs.androidx.lifecycle.viewmodel.ktx)
    implementation(demoLibs.androidx.navigation.fragment.ktx)
    implementation(demoLibs.androidx.navigation.ui.ktx)

    coreLibraryDesugaring(demoLibs.desugarJdkLibs)

    implementation("io.opentelemetry.android:android-agent")    //parent dir
    implementation(demoLibs.androidx.core.ktx)
    implementation(demoLibs.androidx.lifecycle.runtime.ktx)
    implementation(demoLibs.androidx.activity.compose)
    implementation(platform(demoLibs.androidx.compose.bom))
    implementation(demoLibs.androidx.ui)
    implementation(demoLibs.androidx.ui.graphics)
    implementation(demoLibs.androidx.ui.tooling.preview)
    implementation(demoLibs.androidx.material3)

    implementation(demoLibs.opentelemetry.exporter.otlp)

    testImplementation(demoLibs.bundles.junit)
    androidTestImplementation(demoLibs.androidx.junit)
    debugImplementation(demoLibs.androidx.ui.tooling)
    debugImplementation(demoLibs.androidx.ui.test.manifest)
}
