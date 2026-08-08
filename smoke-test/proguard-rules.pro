# Kotlin runs from the optimized target APK because AndroidJUnitRunner executes in its process.
# Keep the runtime methods used only by the test APK.
-keep class kotlin.** { *; }

# The OTLP protobuf artifact includes optional gRPC stubs; this test only decodes HTTP payloads.
-dontwarn io.grpc.**
