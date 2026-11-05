import java.util.Locale
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
    id("com.android.library")
    id("ru.vyarus.animalsniffer")
}

val noDesugarSignature =
    configurations.create("animalsnifferNoDesugarSignature") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

dependencies {
    signature(project(path = ":animal-sniffer-signature", configuration = "generatedSignature"))
    noDesugarSignature(project(path = ":animal-sniffer-signature", configuration = "noDesugarSignature"))
}

val capitalizedVariantNames = mutableListOf<String>()
androidComponents.onVariants { variant ->
    val capitalizedName =
        variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    capitalizedVariantNames.add(capitalizedName)
}

afterEvaluate {
    capitalizedVariantNames.forEach { capitalizedName ->
        val animalSnifferWithNoDesugar = registerAnimalSnifferWithNoDesugar(capitalizedName, noDesugarSignature)
        tasks.named("pre${capitalizedName}Build").configure {
            finalizedBy("animalsniffer$capitalizedName", animalSnifferWithNoDesugar)
        }
        tasks.named("check").configure { dependsOn(animalSnifferWithNoDesugar) }
    }
}

fun registerAnimalSnifferWithNoDesugar(
    capitalizedName: String,
    signatures: Configuration,
): TaskProvider<AnimalSniffer> {
    val mainCheck = tasks.named<AnimalSniffer>("animalsniffer$capitalizedName")
    val reportsDir = layout.buildDirectory.dir("reports/animalsniffer")
    val taskName = "animalsniffer${capitalizedName}WithNoDesugar"
    return tasks.register<AnimalSniffer>(taskName) {
        description = "Checks the $capitalizedName classes against the platform without desugaring."
        group = mainCheck.get().group
        dependsOn(mainCheck)
        targetType = mainCheck.get().targetType
        setSource(mainCheck.get().source)
        sourcesDirs = mainCheck.get().sourcesDirs
        classpath = mainCheck.get().classpath
        animalsnifferClasspath = mainCheck.get().animalsnifferClasspath
        animalsnifferSignatures = signatures
        annotation = animalsniffer.annotation
        ignoreClasses = animalsniffer.ignore
        failWithoutSignatures = true
        reports.configureEach {
            required.convention(true)
            outputLocation.convention(reportsDir.map { it.file("$taskName.$name") })
        }
    }
}

// Any class, method or field annotated with this annotation will be ignored by animal sniffer.
animalsniffer.annotation = "androidx.annotation.RequiresApi"

tasks.withType<AnimalSniffer> {
    // always having declared output makes this task properly participate in tasks up-to-date checks
    reports.text.required.set(true)
}
