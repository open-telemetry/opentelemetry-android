# AndroidJUnitRunner lives in the test APK and references this class from the target APK.
-keep class androidx.tracing.Trace { *; }

# AGP treats dependencies shared by a minified target and its test APK as target-provided. Keep
# Kotlin's runtime available to AndroidJUnitRunner, which executes inside the target process.
-keep class kotlin.** { *; }
