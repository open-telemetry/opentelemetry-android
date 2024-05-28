import com.android.build.api.variant.AndroidComponentsExtension
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import java.util.Locale

plugins {
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}


val isAndroidProject = extensions.findByName("android") != null

if (isAndroidProject) {
    val errorProneConfig = configurations.getByName(ErrorPronePlugin.CONFIGURATION_NAME)
    extensions.getByType(AndroidComponentsExtension::class.java).onVariants {
        it.annotationProcessorConfiguration.extendsFrom(errorProneConfig)
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    errorprone(libs.findLibrary("nullaway").get())
    errorprone(libs.findLibrary("errorprone-core").get())
    errorproneJavac(libs.findLibrary("errorprone-javac").get())
}

nullaway {
    annotatedPackages.add("io.opentelemetry.android")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.errorprone {
            if (name.lowercase(Locale.getDefault()).contains("test")) {
                // just disable all error prone checks for test
                isEnabled.set(false)
                isCompilingTestOnlyCode.set(true)
            } else {
                if (isAndroidProject) {
                    isEnabled.set(true)
                    isCompilingTestOnlyCode.set(false)
                }
            }

            nullaway {
                severity.set(CheckSeverity.ERROR)
                // Prevent generated binding code in demo app from failing the build
                unannotatedSubPackages.add("io.opentelemetry.android.demo.databinding")
            }

            // Builder 'return this;' pattern
            disable("CanIgnoreReturnValueSuggester")
            // Common to avoid an allocation
            disable("MixedMutabilityReturnType")
        }
    }
}
