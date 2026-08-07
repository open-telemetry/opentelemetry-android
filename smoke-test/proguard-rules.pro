# Kotlin runs from the optimized target APK because AndroidJUnitRunner executes in its process.
# Keep the runtime methods used only by the test APK.
-keep class kotlin.** { *; }

# AndroidX Test references these compile-time annotations without requiring them at runtime.
-dontwarn com.google.errorprone.annotations.**

# The OTLP protobuf artifact includes optional gRPC stubs; this test only decodes HTTP payloads.
-dontwarn io.grpc.**
