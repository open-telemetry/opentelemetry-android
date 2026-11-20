import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry Android common test utils"

android {
    namespace = "io.opentelemetry.android.test.common"

    packaging {
        resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
}

dependencies {
    implementation(project(":core"))
    api(project(":agent-api"))
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.sdk.testing)
    api(libs.opentelemetry.kotlin.api)
    implementation(libs.opentelemetry.kotlin.compat)
    api(libs.assertj.core)
    implementation(libs.androidx.core)
    implementation(libs.androidx.junit)
    // Needed to compile OtelGlobalConfigProvider; consumers already provide Robolectric at test runtime.
    compileOnly(libs.robolectric)
}

val koverEnabled = providers.environmentVariable("CI").isPresent ||
        providers.gradleProperty("io.opentelemetry.android.coverage").map { it.toBoolean() }.getOrElse(false)

if (koverEnabled) {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    classes("io.opentelemetry.android.test.common.TestActivity")
                }
            }
        }
    }
}
