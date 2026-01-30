import com.android.build.api.variant.AndroidComponentsExtension
import java.util.Locale
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

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
}

nullaway {
    annotatedPackages.add("io.opentelemetry.android")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
        options.errorprone {
            if (name.lowercase(Locale.getDefault()).contains("test")) {
                // just disable all error prone checks for test
                enabled.set(false)
                compilingTestOnlyCode.set(true)
            } else {
                if (isAndroidProject) {
                    enabled.set(true)
                    compilingTestOnlyCode.set(false)
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
            // Snuck in from NullAway see https://github.com/uber/NullAway/issues/1363#issuecomment-3607506788
            disable("RequireExplicitNullMarking")
        }
    }
}
