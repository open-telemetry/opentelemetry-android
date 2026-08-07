# These dependencies run only in the test APK. Keep their complete runtime because R8 also sees
# the optimized target APK, whose copies may have had classes removed as unreachable from the app.
-keep class kotlin.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# AndroidX Test references these compile-time annotations without requiring them at runtime.
-dontwarn com.google.errorprone.annotations.**

# The OTLP protobuf artifact includes optional gRPC stubs; this test only decodes HTTP payloads.
-dontwarn io.grpc.**
